"""
Train churn prediction models (Logistic Regression + XGBoost) on real or synthetic data.
Run: python train_model.py
Outputs: models/logistic_model.pkl, models/xgboost_model.pkl
"""

import os
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, roc_auc_score, confusion_matrix
from xgboost import XGBClassifier
import joblib
from config import MODEL_DIR, SEED, FEATURE_NAMES, HIGH_RISK_THRESHOLD, MEDIUM_RISK_THRESHOLD
from database_utils import fetch_real_data, fetch_user_engagement_data, create_churn_predictions_table

# Ensure model directory exists
os.makedirs(MODEL_DIR, exist_ok=True)

def generate_enhanced_synthetic_data(n=5000, seed=42):
    """Generate realistic synthetic subscription data with all features."""
    rng = np.random.RandomState(seed)

    data = pd.DataFrame()
    
    # Engagement Features
    data["login_frequency"] = rng.exponential(3.0, n).clip(0, 30)
    data["last_login_days"] = rng.exponential(15.0, n).clip(0, 180).astype(int)
    data["courses_completed"] = rng.poisson(4, n).clip(0, 50)
    data["active_courses"] = rng.poisson(1.5, n).clip(0, 10)
    
    # Payment Features
    data["payment_success_rate"] = rng.beta(8, 2, n).clip(0, 1)
    data["failed_payments_count"] = rng.poisson(0.5, n).clip(0, 10)
    
    # Total spent: more for long-term users and higher tiers
    base_spent = rng.exponential(50, n).clip(0, 500)
    data["total_spent"] = base_spent * (1 + rng.gamma(2, 1, n))
    
    # Last payment: correlated with engagement
    data["last_payment_days"] = rng.exponential(30, n).clip(0, 180).astype(int)
    
    # Subscription Features
    data["subscription_duration_days"] = rng.exponential(120, n).clip(1, 730).astype(int)
    data["days_until_expiry"] = rng.exponential(30, n).clip(-30, 180).astype(int)
    
    # Plan tier: weighted distribution (more standard users)
    plan_choices = np.random.choice([0, 1, 2], n, p=[0.6, 0.3, 0.1])
    data["plan_tier"] = plan_choices
    
    # Auto-renew: more likely for engaged users
    data["auto_renew_enabled"] = (rng.random(n) < (0.3 + 0.4 * (1 - data["days_until_expiry"] / 180))).astype(int)
    
    # Renewal count: based on duration
    data["renewal_count"] = (data["subscription_duration_days"] / 90).clip(0, 10).astype(int)
    
    # Communication Features
    data["email_open_rate"] = rng.beta(3, 4, n).clip(0, 1)
    data["email_click_rate"] = (data["email_open_rate"] * rng.beta(2, 5, n)).clip(0, 1)
    
    # Activity Features
    data["quiz_completion_rate"] = rng.beta(8, 2, n).clip(0, 1)
    data["forum_posts_count"] = rng.poisson(2, n).clip(0, 100)
    data["events_attended"] = rng.poisson(1, n).clip(0, 20)
    data["support_tickets_count"] = rng.poisson(0.5, n).clip(0, 10)
    
    # Incentive Features
    data["discount_codes_used"] = rng.poisson(0.5, n).clip(0, 5)
    data["invoice_frequency"] = rng.exponential(30, n).clip(7, 90).astype(int)
    
    # Churn probability based on realistic feature relationships
    # Higher churn for: low engagement, payment issues, nearing expiry, lower tiers
    logit = (
        -1.5  # Base intercept
        - 0.3 * data["login_frequency"]  # More login = less churn
        + 0.04 * data["last_login_days"]  # Longer since login = more churn
        - 0.1 * data["courses_completed"]  # More courses = less churn
        - 0.2 * data["active_courses"]  # More active courses = less churn
        - 2.0 * data["payment_success_rate"]  # Better payment = less churn
        + 0.4 * data["failed_payments_count"]  # More failures = more churn
        - 0.002 * data["subscription_duration_days"]  # Longer tenure = less churn
        - 1.5 * data["email_open_rate"]  # Better email engagement = less churn
        - 1.0 * data["email_click_rate"]  # More clicks = less churn
        + 0.5 * data["days_until_expiry"] / 100  # Closer to expiry = more churn
        - 0.8 * data["plan_tier"]  # Higher tier = less churn
        + 0.5 * (1 - data["auto_renew_enabled"])  # No auto-renew = more churn
        - 0.01 * data["total_spent"] / 100  # More spent = less churn
        - 0.01 * data["renewal_count"]  # More renewals = less churn
        - 0.5 * data["quiz_completion_rate"]  # Better quiz completion = less churn
        - 0.02 * data["forum_posts_count"]  # More forum activity = less churn
        - 0.1 * data["events_attended"]  # More events = less churn
        + 0.3 * data["support_tickets_count"]  # More support tickets = more churn (pain)
        + rng.normal(0, 0.5, n)  # Random noise
    )
    
    prob = 1.0 / (1.0 + np.exp(-logit))
    data["churned"] = (rng.random(n) < prob).astype(int)
    
    print(f"Generated {n} synthetic samples — churn rate: {data['churned'].mean():.2%}")
    return data

def prepare_data(df):
    """
    Prepare data for training by handling missing values and merging engagement data.
    """
    # Handle missing values
    df = df.fillna(0)
    
    # Ensure all feature columns exist
    for feature in FEATURE_NAMES:
        if feature not in df.columns:
            df[feature] = 0
    
    # Calculate email_click_rate if missing
    if 'email_click_rate' in df.columns and 'email_open_rate' in df.columns:
        df['email_click_rate'] = df['email_click_rate'].fillna(df['email_open_rate'] * 0.3)
    
    return df

def train_and_save():
    """Train models on real or synthetic data and save them."""
    print("=" * 60)
    print("CHURN PREDICTION MODEL TRAINING")
    print("=" * 60)
    
    # Try to fetch real data first
    df = fetch_real_data()
    
    if df is None or len(df) < 100:
        print("\n⚠ Not enough real data available. Using synthetic data.")
        df = generate_enhanced_synthetic_data(N_SAMPLES=5000)
    else:
        print(f"\n✓ Using {len(df)} real records from database")
        # Merge with engagement data if available
        user_ids = df['user_id'].unique()
        engagement_df = fetch_user_engagement_data(user_ids)
        df = pd.merge(df, engagement_df, on='user_id', how='left')
    
    # Prepare data
    df = prepare_data(df)
    
    # Separate features and target
    X = df[FEATURE_NAMES]
    y = df["churned"]
    
    print(f"\nFeatures used ({len(FEATURE_NAMES)}):")
    for i, feature in enumerate(FEATURE_NAMES, 1):
        print(f"  {i}. {feature}")
    
    print(f"\nTarget distribution:")
    print(f"  Not churned (0): {sum(y == 0)} ({(sum(y == 0) / len(y) * 100):.1f}%)")
    print(f"  Churned (1): {sum(y == 1)} ({(sum(y == 1) / len(y) * 100):.1f}%)")
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=SEED, stratify=y
    )
    
    print(f"\nData split: {len(X_train)} training, {len(X_test)} test samples")
    
    # --- Logistic Regression ---
    print("\n" + "=" * 60)
    print("LOGISTIC REGRESSION")
    print("=" * 60)
    lr = LogisticRegression(max_iter=1000, random_state=SEED, class_weight='balanced')
    lr.fit(X_train, y_train)
    
    lr_pred = lr.predict(X_test)
    lr_prob = lr.predict_proba(X_test)[:, 1]
    
    print("\nClassification Report:")
    print(classification_report(y_test, lr_pred, target_names=['Not Churned', 'Churned']))
    print(f"ROC-AUC: {roc_auc_score(y_test, lr_prob):.4f}")
    
    lr_path = os.path.join(MODEL_DIR, "logistic_model.pkl")
    joblib.dump(lr, lr_path)
    print(f"\n✓ Saved Logistic Regression model: {lr_path}")
    
    # --- XGBoost ---
    print("\n" + "=" * 60)
    print("XGBOOST")
    print("=" * 60)
    xgb = XGBClassifier(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.1,
        random_state=SEED,
        use_label_encoder=False,
        eval_metric="logloss",
        scale_pos_weight=(y_train == 0).sum() / (y_train == 1).sum()  # Handle imbalance
    )
    xgb.fit(X_train, y_train)
    
    xgb_pred = xgb.predict(X_test)
    xgb_prob = xgb.predict_proba(X_test)[:, 1]
    
    print("\nClassification Report:")
    print(classification_report(y_test, xgb_pred, target_names=['Not Churned', 'Churned']))
    print(f"ROC-AUC: {roc_auc_score(y_test, xgb_prob):.4f}")
    
    xgb_path = os.path.join(MODEL_DIR, "xgboost_model.pkl")
    joblib.dump(xgb, xgb_path)
    print(f"\n✓ Saved XGBoost model: {xgb_path}")
    
    # --- Feature Importance (XGBoost) ---
    print("\n" + "=" * 60)
    print("FEATURE IMPORTANCE (XGBoost)")
    print("=" * 60)
    importances = dict(zip(FEATURE_NAMES, xgb.feature_importances_))
    sorted_importances = sorted(importances.items(), key=lambda x: -x[1])
    
    for i, (feat, imp) in enumerate(sorted_importances, 1):
        print(f"  {i:2}. {feat:30s} {imp:.4f}")
    
    # Save feature names for the Flask app
    joblib.dump(FEATURE_NAMES, os.path.join(MODEL_DIR, "feature_names.pkl"))
    print(f"\n✓ Saved feature names: {os.path.join(MODEL_DIR, 'feature_names.pkl')}")
    
    # Save model metadata
    metadata = {
        "model_type": "XGBoost",
        "feature_names": FEATURE_NAMES,
        "n_features": len(FEATURE_NAMES),
        "n_samples": len(df),
        "churn_rate": float(y.mean()),
        "high_risk_threshold": HIGH_RISK_THRESHOLD,
        "medium_risk_threshold": MEDIUM_RISK_THRESHOLD,
        "roc_auc": float(roc_auc_score(y_test, xgb_prob)),
        "trained_at": pd.Timestamp.now().isoformat()
    }
    joblib.dump(metadata, os.path.join(MODEL_DIR, "model_metadata.pkl"))
    print(f"✓ Saved model metadata: {os.path.join(MODEL_DIR, 'model_metadata.pkl')}")
    
    # Create database table for predictions
    print("\n" + "=" * 60)
    print("DATABASE SETUP")
    print("=" * 60)
    create_churn_predictions_table()
    
    print("\n" + "=" * 60)
    print("✓ TRAINING COMPLETE!")
    print("=" * 60)
    print(f"Models saved to: {MODEL_DIR}/")
    print(f"  - logistic_model.pkl")
    print(f"  - xgboost_model.pkl")
    print(f"  - feature_names.pkl")
    print(f"  - model_metadata.pkl")
    print(f"\nStart the Flask API with: python app.py")

if __name__ == "__main__":
    train_and_save()