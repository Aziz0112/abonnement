"""
Train churn prediction models (Logistic Regression + XGBoost) on synthetic data.
Run: python train_model.py
Outputs: models/logistic_model.pkl, models/xgboost_model.pkl
"""

import os
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, roc_auc_score
from xgboost import XGBClassifier
import joblib

SEED = 42
N_SAMPLES = 5000
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

FEATURE_NAMES = [
    "login_frequency",
    "last_login_days",
    "courses_completed",
    "active_courses",
    "payment_success_rate",
    "failed_payments_count",
    "subscription_duration_days",
    "email_open_rate",
    "email_click_rate",
]


def generate_synthetic_data(n=N_SAMPLES, seed=SEED):
    """Generate realistic synthetic subscription data with a churned label."""
    rng = np.random.RandomState(seed)

    data = pd.DataFrame()
    data["login_frequency"] = rng.exponential(3.0, n).clip(0, 30)
    data["last_login_days"] = rng.exponential(15.0, n).clip(0, 180).astype(int)
    data["courses_completed"] = rng.poisson(4, n).clip(0, 50)
    data["active_courses"] = rng.poisson(1.5, n).clip(0, 10)
    data["payment_success_rate"] = rng.beta(8, 2, n).clip(0, 1)
    data["failed_payments_count"] = rng.poisson(0.5, n).clip(0, 10)
    data["subscription_duration_days"] = rng.exponential(120, n).clip(1, 730).astype(int)
    data["email_open_rate"] = rng.beta(3, 4, n).clip(0, 1)
    data["email_click_rate"] = (data["email_open_rate"] * rng.beta(2, 5, n)).clip(0, 1)

    # Churn probability based on realistic feature relationships
    logit = (
        -1.5
        - 0.3 * data["login_frequency"]
        + 0.04 * data["last_login_days"]
        - 0.1 * data["courses_completed"]
        - 0.2 * data["active_courses"]
        - 2.0 * data["payment_success_rate"]
        + 0.4 * data["failed_payments_count"]
        - 0.002 * data["subscription_duration_days"]
        - 1.5 * data["email_open_rate"]
        - 1.0 * data["email_click_rate"]
        + rng.normal(0, 0.5, n)
    )
    prob = 1.0 / (1.0 + np.exp(-logit))
    data["churned"] = (rng.random(n) < prob).astype(int)

    print(f"Generated {n} samples — churn rate: {data['churned'].mean():.2%}")
    return data


def train_and_save():
    os.makedirs(MODEL_DIR, exist_ok=True)

    df = generate_synthetic_data()
    X = df[FEATURE_NAMES]
    y = df["churned"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=SEED, stratify=y
    )

    # --- Logistic Regression ---
    print("\n=== Logistic Regression ===")
    lr = LogisticRegression(max_iter=1000, random_state=SEED)
    lr.fit(X_train, y_train)
    lr_pred = lr.predict(X_test)
    lr_prob = lr.predict_proba(X_test)[:, 1]
    print(classification_report(y_test, lr_pred))
    print(f"ROC-AUC: {roc_auc_score(y_test, lr_prob):.4f}")

    lr_path = os.path.join(MODEL_DIR, "logistic_model.pkl")
    joblib.dump(lr, lr_path)
    print(f"Saved: {lr_path}")

    # --- XGBoost ---
    print("\n=== XGBoost ===")
    xgb = XGBClassifier(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.1,
        random_state=SEED,
        use_label_encoder=False,
        eval_metric="logloss",
    )
    xgb.fit(X_train, y_train)
    xgb_pred = xgb.predict(X_test)
    xgb_prob = xgb.predict_proba(X_test)[:, 1]
    print(classification_report(y_test, xgb_pred))
    print(f"ROC-AUC: {roc_auc_score(y_test, xgb_prob):.4f}")

    xgb_path = os.path.join(MODEL_DIR, "xgboost_model.pkl")
    joblib.dump(xgb, xgb_path)
    print(f"Saved: {xgb_path}")

    # --- Feature Importance (XGBoost) ---
    print("\n=== Feature Importance (XGBoost) ===")
    importances = dict(zip(FEATURE_NAMES, xgb.feature_importances_))
    for feat, imp in sorted(importances.items(), key=lambda x: -x[1]):
        print(f"  {feat:30s} {imp:.4f}")

    # Save feature names for the Flask app
    joblib.dump(FEATURE_NAMES, os.path.join(MODEL_DIR, "feature_names.pkl"))
    print("\nDone! Models saved to models/ directory.")


if __name__ == "__main__":
    train_and_save()
