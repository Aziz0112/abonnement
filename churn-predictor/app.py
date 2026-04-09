"""
Enhanced Flask API for churn prediction with dynamic thresholds, recommendations, and SHAP explainability.
Run: python app.py
Endpoints:
  POST /predict            — single user prediction with recommendations
  POST /predict-batch      — batch predictions
  POST /predict-history    — churn probability trend for a user
  GET  /health             — health check
  GET  /model-info         — model metadata
"""

import os
import numpy as np
import pandas as pd
import joblib
from flask import Flask, request, jsonify
from flask_cors import CORS
import shap
from datetime import datetime, timedelta

from config import (
    MODEL_DIR, FEATURE_NAMES, HIGH_RISK_THRESHOLD, 
    MEDIUM_RISK_THRESHOLD, API_HOST, API_PORT, API_DEBUG
)
from database_utils import get_latest_user_features, save_prediction_to_database

app = Flask(__name__)
CORS(app)

# Load model and feature names at startup
model = None
feature_names = None
model_metadata = None
shap_explainer = None


def load_model():
    """Load model, feature names, metadata, and SHAP explainer."""
    global model, feature_names, model_metadata, shap_explainer
    
    # Load model (prefer XGBoost, fallback to Logistic Regression)
    xgb_path = os.path.join(MODEL_DIR, "xgboost_model.pkl")
    lr_path = os.path.join(MODEL_DIR, "logistic_model.pkl")
    
    if os.path.exists(xgb_path):
        model = joblib.load(xgb_path)
        print("✓ Loaded XGBoost model")
        
        # Create SHAP explainer for XGBoost
        try:
            shap_explainer = shap.TreeExplainer(model)
            print("✓ Initialized SHAP explainer")
        except Exception as e:
            print(f"⚠ Could not initialize SHAP explainer: {e}")
            
    elif os.path.exists(lr_path):
        model = joblib.load(lr_path)
        print("✓ Loaded Logistic Regression model (fallback)")
    else:
        raise FileNotFoundError("No trained model found in models/. Run train_model.py first.")
    
    # Load feature names
    names_path = os.path.join(MODEL_DIR, "feature_names.pkl")
    if os.path.exists(names_path):
        feature_names = joblib.load(names_path)
    else:
        feature_names = FEATURE_NAMES
        print(f"⚠ Using default feature names ({len(feature_names)} features)")
    
    # Load model metadata
    metadata_path = os.path.join(MODEL_DIR, "model_metadata.pkl")
    if os.path.exists(metadata_path):
        model_metadata = joblib.load(metadata_path)
        print(f"✓ Loaded model metadata (trained at: {model_metadata.get('trained_at', 'unknown')})")


def get_risk_level(prob, context=None):
    """
    Dynamic risk levels based on business context.
    """
    high_threshold = HIGH_RISK_THRESHOLD
    medium_threshold = MEDIUM_RISK_THRESHOLD
    
    # Adjust thresholds based on context
    if context and context.get('urgent_retention'):
        # Aggressive mode during campaigns
        high_threshold = 0.5
        medium_threshold = 0.2
    elif context and context.get('conservative'):
        # Conservative mode for high-value customers
        high_threshold = 0.8
        medium_threshold = 0.4
    
    if prob > high_threshold:
        return "HIGH"
    elif prob > medium_threshold:
        return "MEDIUM"
    else:
        return "LOW"


def get_feature_importance():
    """Extract feature importance from the loaded model."""
    if hasattr(model, "feature_importances_"):
        # XGBoost or tree-based models
        return dict(zip(feature_names, [round(float(v), 4) for v in model.feature_importances_]))
    elif hasattr(model, "coef_"):
        # Logistic Regression
        coefs = np.abs(model.coef_[0])
        normalized = coefs / coefs.sum()
        return dict(zip(feature_names, [round(float(v), 4) for v in normalized]))
    return {}


def get_retention_recommendations(features_dict, prob, risk_level):
    """
    Provide actionable recommendations based on features and risk level.
    """
    recommendations = []
    
    # Priority-based recommendations
    if risk_level == "HIGH":
        recommendations.append({
            "priority": "CRITICAL",
            "action": "Immediate outreach needed - customer at high risk of churning",
            "type": "support"
        })
    
    # Engagement-based recommendations
    if features_dict.get('last_login_days', 0) > 30:
        recommendations.append({
            "priority": "HIGH" if risk_level == "HIGH" else "MEDIUM",
            "action": f"User hasn't logged in for {features_dict['last_login_days']} days. Send re-engagement email with personalized course recommendations.",
            "type": "email"
        })
    
    if features_dict.get('login_frequency', 0) < 2:
        recommendations.append({
            "priority": "MEDIUM",
            "action": "Low login frequency. Consider gamification or progress reminders.",
            "type": "feature"
        })
    
    # Payment-based recommendations
    if features_dict.get('payment_success_rate', 1.0) < 0.8:
        recommendations.append({
            "priority": "CRITICAL",
            "action": "Low payment success rate. Contact user about payment issues and offer alternative methods.",
            "type": "support"
        })
    
    if features_dict.get('failed_payments_count', 0) > 2:
        recommendations.append({
            "priority": "HIGH",
            "action": f"User has {features_dict['failed_payments_count']} failed payments. Review payment history.",
            "type": "support"
        })
    
    # Subscription-based recommendations
    if not features_dict.get('auto_renew_enabled', False):
        recommendations.append({
            "priority": "HIGH",
            "action": "Auto-renew is disabled. Offer 10% discount for enabling auto-renewal.",
            "type": "incentive"
        })
    
    if features_dict.get('days_until_expiry', 0) < 7 and features_dict.get('days_until_expiry', 0) > 0:
        recommendations.append({
            "priority": "HIGH",
            "action": f"Subscription expires in {features_dict['days_until_expiry']} days. Send expiry reminder with renewal incentive.",
            "type": "email"
        })
    
    # Email engagement recommendations
    if features_dict.get('email_open_rate', 0) < 0.2:
        recommendations.append({
            "priority": "MEDIUM",
            "action": "Low email open rate. Optimize subject lines and send time based on user activity.",
            "type": "email_strategy"
        })
    
    # Activity-based recommendations
    if features_dict.get('courses_completed', 0) == 0:
        recommendations.append({
            "priority": "MEDIUM",
            "action": "User hasn't completed any courses. Send onboarding guide and beginner course suggestions.",
            "type": "feature"
        })
    
    if features_dict.get('support_tickets_count', 0) > 5:
        recommendations.append({
            "priority": "MEDIUM",
            "action": f"User has opened {features_dict['support_tickets_count']} support tickets. Review for recurring issues.",
            "type": "support"
        })
    
    # Plan tier recommendations
    if features_dict.get('plan_tier', 0) == 0 and features_dict.get('total_spent', 0) > 100:
        recommendations.append({
            "priority": "LOW",
            "action": "User is on freemium but has spent significantly. Consider upgrading offer.",
            "type": "sales"
        })
    
    return recommendations[:10]  # Return top 10 recommendations


def get_shap_explanation(features_dict):
    """
    Explain why a specific prediction was made using SHAP values.
    """
    if shap_explainer is None:
        return []
    
    try:
        feature_vector = np.array([[features_dict.get(f, 0.0) for f in feature_names]])
        shap_values = shap_explainer.shap_values(feature_vector)
        
        # Format explanation
        explanation = []
        for i, (name, value) in enumerate(zip(feature_names, shap_values[0])):
            explanation.append({
                "feature": name,
                "contribution": float(value),
                "value": float(feature_vector[0][i]),
                "impact": "increases" if value > 0 else "decreases"
            })
        
        return sorted(explanation, key=lambda x: abs(x['contribution']), reverse=True)[:10]
    except Exception as e:
        print(f"Error generating SHAP explanation: {e}")
        return []


def generate_narrative_insight(prediction, features):
    """Generate human-readable explanation of the prediction."""
    risk_level = prediction['risk_level']
    prob = prediction['churn_probability']
    
    narrative = f"This user has a {risk_level.lower()} risk of churning "
    narrative += f"(probability: {prob:.1%}).\n\n"
    
    # Add top contributors
    if 'shap_explanation' in prediction and prediction['shap_explanation']:
        narrative += "Key factors:\n"
        for feature in prediction['shap_explanation'][:3]:
            if feature['impact'] == 'increases':
                narrative += f"• High {feature['feature']} ({feature['value']:.2f}) increases churn risk\n"
            else:
                narrative += f"• Good {feature['feature']} ({feature['value']:.2f}) reduces churn risk\n"
    
    # Add recommendation count
    if 'recommendations' in prediction:
        narrative += f"\n{len(prediction['recommendations'])} retention actions available."
    
    return narrative


def predict_single(features_dict, context=None, include_explanations=True):
    """
    Predict churn for a single feature set with full analysis.
    """
    feature_vector = np.array([[features_dict.get(f, 0.0) for f in feature_names]])
    prob = float(model.predict_proba(feature_vector)[0][1])
    risk_level = get_risk_level(prob, context)
    
    result = {
        "churn_probability": round(prob, 4),
        "risk_level": risk_level,
        "timestamp": datetime.now().isoformat(),
    }
    
    # Add feature importance
    result["feature_importance"] = get_feature_importance()
    
    # Add SHAP explanation if requested and available
    if include_explanations:
        shap_explanation = get_shap_explanation(features_dict)
        if shap_explanation:
            result["shap_explanation"] = shap_explanation
    
    # Add retention recommendations
    result["recommendations"] = get_retention_recommendations(features_dict, prob, risk_level)
    
    # Add narrative insight
    result["narrative"] = generate_narrative_insight(result, features_dict)
    
    return result


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "ok",
        "model_loaded": model is not None,
        "features_count": len(feature_names) if feature_names else 0,
        "timestamp": datetime.now().isoformat()
    })


@app.route("/model-info", methods=["GET"])
def model_info():
    """Get model metadata."""
    if model_metadata:
        return jsonify(model_metadata)
    else:
        return jsonify({
            "model_type": type(model).__name__ if model else "Unknown",
            "features": feature_names,
            "n_features": len(feature_names) if feature_names else 0
        })


@app.route("/predict", methods=["POST"])
def predict():
    """
    Predict churn for a single user with full analysis.
    Body: { 
        "userId": 123,
        "features": { "login_frequency": 2.5, "last_login_days": 10, ... },
        "context": { "urgent_retention": false }
    }
    """
    data = request.get_json(force=True)
    if not data:
        return jsonify({"error": "Request body is required"}), 400
    
    try:
        user_id = data.get("userId")
        features = data.get("features", {})
        context = data.get("context", {})
        include_explanations = data.get("include_explanations", True)
        
        # Predict
        result = predict_single(features, context, include_explanations)
        result["userId"] = user_id
        
        # Save to database if userId provided
        if user_id:
            save_prediction_to_database(user_id, {
                "churn_probability": result["churn_probability"],
                "risk_level": result["risk_level"]
            })
        
        return jsonify(result)
    except Exception as e:
        print(f"Error in /predict: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/predict-batch", methods=["POST"])
def predict_batch():
    """
    Predict churn for multiple users.
    Body: { 
        "users": [ { "userId": 1, "features": { ... } }, ... ],
        "context": { "urgent_retention": false }
    }
    """
    data = request.get_json(force=True)
    if not data or "users" not in data:
        return jsonify({"error": "'users' array is required"}), 400
    
    try:
        context = data.get("context", {})
        results = []
        importance = get_feature_importance()
        
        for user_entry in data["users"]:
            user_id = user_entry.get("userId")
            features = user_entry.get("features", {})
            
            # Predict
            feature_vector = np.array([[features.get(f, 0.0) for f in feature_names]])
            prob = float(model.predict_proba(feature_vector)[0][1])
            risk_level = get_risk_level(prob, context)
            
            results.append({
                "userId": user_id,
                "churn_probability": round(prob, 4),
                "risk_level": risk_level,
            })
            
            # Save to database
            if user_id:
                save_prediction_to_database(user_id, {
                    "churn_probability": prob,
                    "risk_level": risk_level
                })
        
        # Sort by churn probability (highest risk first)
        results.sort(key=lambda x: x["churn_probability"], reverse=True)
        
        return jsonify({
            "predictions": results,
            "feature_importance": importance,
            "timestamp": datetime.now().isoformat(),
            "total_users": len(results)
        })
    except Exception as e:
        print(f"Error in /predict-batch: {e}")
        return jsonify({"error": str(e)}), 500


@app.route("/predict-history", methods=["GET"])
def predict_history():
    """
    Get churn probability trend for a user over time.
    Query params: userId, days (default: 30)
    """
    user_id = request.args.get("userId")
    days = int(request.args.get("days", 30))
    
    if not user_id:
        return jsonify({"error": "userId parameter is required"}), 400
    
    try:
        # TODO: Implement actual historical data retrieval
        # For now, return synthetic trend data
        
        start_date = datetime.now() - timedelta(days=days)
        trend = []
        
        # Generate synthetic trend with some noise
        base_prob = 0.3 + np.random.random() * 0.4
        for i in range(days):
            date = start_date + timedelta(days=i)
            prob = base_prob + np.random.normal(0, 0.05)
            prob = max(0, min(1, prob))
            
            trend.append({
                "date": date.strftime("%Y-%m-%d"),
                "churn_probability": round(prob, 4),
                "risk_level": get_risk_level(prob)
            })
        
        # Calculate trend direction
        if len(trend) > 1:
            first_prob = trend[0]["churn_probability"]
            last_prob = trend[-1]["churn_probability"]
            if last_prob > first_prob + 0.1:
                trend_direction = "increasing"
            elif last_prob < first_prob - 0.1:
                trend_direction = "decreasing"
            else:
                trend_direction = "stable"
        else:
            trend_direction = "unknown"
        
        return jsonify({
            "userId": user_id,
            "trend": trend,
            "trend_direction": trend_direction,
            "period_days": days,
            "current_probability": trend[-1]["churn_probability"] if trend else 0,
            "timestamp": datetime.now().isoformat()
        })
    except Exception as e:
        print(f"Error in /predict-history: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    print("=" * 60)
    print("CHURN PREDICTION API")
    print("=" * 60)
    load_model()
    print("=" * 60)
    print("API Endpoints:")
    print("  GET  /health            - Health check")
    print("  GET  /model-info        - Model metadata")
    print("  POST /predict           - Single user prediction")
    print("  POST /predict-batch     - Batch predictions")
    print("  GET  /predict-history   - User churn trend")
    print("=" * 60)
    print(f"Starting server on {API_HOST}:{API_PORT}")
    print("=" * 60)
    
    app.run(host=API_HOST, port=API_PORT, debug=API_DEBUG)