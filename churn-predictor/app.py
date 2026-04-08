"""
Flask API for churn prediction.
Run: python app.py
Endpoints:
  POST /predict       — single user prediction
  POST /predict-batch — batch predictions
  GET  /health        — health check
"""

import os
import numpy as np
import joblib
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

# Load model and feature names at startup
model = None
feature_names = None


def load_model():
    global model, feature_names
    xgb_path = os.path.join(MODEL_DIR, "xgboost_model.pkl")
    lr_path = os.path.join(MODEL_DIR, "logistic_model.pkl")
    names_path = os.path.join(MODEL_DIR, "feature_names.pkl")

    if os.path.exists(xgb_path):
        model = joblib.load(xgb_path)
        print("Loaded XGBoost model")
    elif os.path.exists(lr_path):
        model = joblib.load(lr_path)
        print("Loaded Logistic Regression model (fallback)")
    else:
        raise FileNotFoundError("No trained model found in models/. Run train_model.py first.")

    if os.path.exists(names_path):
        feature_names = joblib.load(names_path)
    else:
        feature_names = [
            "login_frequency", "last_login_days", "courses_completed",
            "active_courses", "payment_success_rate", "failed_payments_count",
            "subscription_duration_days", "email_open_rate", "email_click_rate",
        ]


def get_risk_level(prob):
    if prob > 0.7:
        return "HIGH"
    elif prob > 0.3:
        return "MEDIUM"
    else:
        return "LOW"


def get_feature_importance():
    """Extract feature importance from the loaded model."""
    if hasattr(model, "feature_importances_"):
        return dict(zip(feature_names, [round(float(v), 4) for v in model.feature_importances_]))
    elif hasattr(model, "coef_"):
        coefs = np.abs(model.coef_[0])
        normalized = coefs / coefs.sum()
        return dict(zip(feature_names, [round(float(v), 4) for v in normalized]))
    return {}


def predict_single(features_dict):
    """Predict churn for a single feature set."""
    feature_vector = np.array([[features_dict.get(f, 0.0) for f in feature_names]])
    prob = float(model.predict_proba(feature_vector)[0][1])
    return {
        "churn_probability": round(prob, 4),
        "risk_level": get_risk_level(prob),
        "feature_importance": get_feature_importance(),
    }


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model_loaded": model is not None})


@app.route("/predict", methods=["POST"])
def predict():
    """Predict churn for a single user.
    Body: { "login_frequency": 2.5, "last_login_days": 10, ... }
    """
    data = request.get_json(force=True)
    if not data:
        return jsonify({"error": "Request body is required"}), 400

    try:
        result = predict_single(data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/predict-batch", methods=["POST"])
def predict_batch():
    """Predict churn for multiple users.
    Body: { "users": [ { "userId": 1, "features": { ... } }, ... ] }
    """
    data = request.get_json(force=True)
    if not data or "users" not in data:
        return jsonify({"error": "'users' array is required"}), 400

    try:
        results = []
        importance = get_feature_importance()
        for user_entry in data["users"]:
            user_id = user_entry.get("userId")
            features = user_entry.get("features", {})
            feature_vector = np.array([[features.get(f, 0.0) for f in feature_names]])
            prob = float(model.predict_proba(feature_vector)[0][1])
            results.append({
                "userId": user_id,
                "churn_probability": round(prob, 4),
                "risk_level": get_risk_level(prob),
            })

        results.sort(key=lambda x: x["churn_probability"], reverse=True)

        return jsonify({
            "predictions": results,
            "feature_importance": importance,
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    load_model()
    app.run(host="0.0.0.0", port=5000, debug=True)
