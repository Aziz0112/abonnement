"""
Configuration file for churn predictor.
Database connection and model settings.
"""

import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database Configuration
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "abonnements")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "password")

# Database URL for SQLAlchemy
DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Model Configuration
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
SEED = 42
N_SAMPLES = 5000

# Risk Thresholds
HIGH_RISK_THRESHOLD = 0.7
MEDIUM_RISK_THRESHOLD = 0.3

# API Configuration
API_HOST = os.getenv("API_HOST", "0.0.0.0")
API_PORT = int(os.getenv("API_PORT", "5000"))
API_DEBUG = os.getenv("API_DEBUG", "True").lower() == "true"

# Feature Names
FEATURE_NAMES = [
    # Engagement Features
    "login_frequency",
    "last_login_days",
    "courses_completed",
    "active_courses",
    
    # Payment Features
    "payment_success_rate",
    "failed_payments_count",
    "total_spent",
    "last_payment_days",
    
    # Subscription Features
    "subscription_duration_days",
    "days_until_expiry",
    "plan_tier",
    "auto_renew_enabled",
    "renewal_count",
    
    # Communication Features
    "email_open_rate",
    "email_click_rate",
    
    # Activity Features
    "quiz_completion_rate",
    "forum_posts_count",
    "events_attended",
    "support_tickets_count",
    
    # Incentive Features
    "discount_codes_used",
    "invoice_frequency",
]