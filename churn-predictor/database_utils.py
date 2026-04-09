"""
Database utilities for fetching real subscription data.
"""

import os
from datetime import datetime, timedelta
import pandas as pd
from sqlalchemy import create_engine, text
from config import DATABASE_URL

def get_database_engine():
    """Create SQLAlchemy engine for database connection."""
    return create_engine(DATABASE_URL, pool_pre_ping=True)

def fetch_real_data():
    """
    Fetch real subscription data from PostgreSQL database.
    Returns a DataFrame with all features for churn prediction.
    """
    engine = get_database_engine()
    
    query = """
    WITH subscription_stats AS (
        SELECT 
            us.id,
            us.user_id,
            us.plan_id,
            us.subscribed,
            us.expires,
            us.status,
            us.auto_renew,
            us.stripe_customer_id,
            sp.name as plan_name,
            sp.price as plan_price,
            sp.duration as plan_duration_months
        FROM usersubscriptions us
        JOIN subscriptionplans sp ON us.plan_id = sp.id
    ),
    invoice_stats AS (
        SELECT 
            subscription_id,
            COUNT(*) as invoice_count,
            SUM(amount) as total_spent,
            AVG(CASE WHEN paid THEN 1 ELSE 0 END) as payment_success_rate,
            SUM(CASE WHEN paid = FALSE THEN 1 ELSE 0 END) as failed_payments_count,
            MAX(issued_at) as last_payment_date,
            MIN(issued_at) as first_payment_date
        FROM invoices
        GROUP BY subscription_id
    ),
    invoice_frequency AS (
        SELECT 
            subscription_id,
            CASE 
                WHEN COUNT(*) > 1 THEN 
                    EXTRACT(DAY FROM (MAX(issued_at) - MIN(issued_at))) / NULLIF(COUNT(*) - 1, 0)
                ELSE 0 
            END as avg_days_between_invoices
        FROM invoices
        GROUP BY subscription_id
    ),
    discount_usage AS (
        SELECT 
            subscription_id,
            COUNT(DISTINCT dc.code) as discount_codes_used
        FROM invoices i
        LEFT JOIN discount_codes dc ON i.discount_code_id = dc.id
        GROUP BY subscription_id
    )
    SELECT 
        ss.id,
        ss.user_id,
        
        -- Subscription Features
        EXTRACT(DAY FROM (NOW() - ss.subscribed)) as subscription_duration_days,
        EXTRACT(DAY FROM (ss.expires - NOW())) as days_until_expiry,
        CASE 
            WHEN ss.plan_name = 'FREEMIUM' THEN 0
            WHEN ss.plan_name = 'STANDARD' THEN 1
            WHEN ss.plan_name = 'PREMIUM' THEN 2
            ELSE 0
        END as plan_tier,
        ss.auto_renew as auto_renew_enabled,
        
        -- Payment Features
        COALESCE(istats.invoice_count, 0) as renewal_count,
        COALESCE(istats.total_spent, 0) as total_spent,
        COALESCE(istats.payment_success_rate, 1.0) as payment_success_rate,
        COALESCE(istats.failed_payments_count, 0) as failed_payments_count,
        EXTRACT(DAY FROM (NOW() - COALESCE(istats.last_payment_date, ss.subscribed))) as last_payment_days,
        COALESCE(ifreq.avg_days_between_invoices, 30) as invoice_frequency,
        
        -- Incentive Features
        COALESCE(dstats.discount_codes_used, 0) as discount_codes_used,
        
        -- Churn Label (CANCELLED status)
        CASE WHEN ss.status = 'CANCELLED' THEN 1 ELSE 0 END as churned
        
    FROM subscription_stats ss
    LEFT JOIN invoice_stats istats ON ss.id = istats.subscription_id
    LEFT JOIN invoice_frequency ifreq ON ss.id = ifreq.subscription_id
    LEFT JOIN discount_usage dstats ON ss.id = dstats.subscription_id
    WHERE ss.subscribed IS NOT NULL
    """
    
    try:
        df = pd.read_sql(query, engine)
        print(f"✓ Fetched {len(df)} real subscription records from database")
        return df
    except Exception as e:
        print(f"✗ Error fetching data from database: {e}")
        print("⚠ Falling back to synthetic data generation")
        return None

def fetch_user_engagement_data(user_ids):
    """
    Fetch engagement data from user service for specific users.
    This would connect to your user microservice via API or database.
    """
    # TODO: Implement actual connection to user service
    # For now, return synthetic engagement data
    
    import numpy as np
    rng = np.random.RandomState(42)
    
    data = []
    for user_id in user_ids:
        data.append({
            "user_id": user_id,
            "login_frequency": rng.exponential(3.0).clip(0, 30),
            "last_login_days": rng.exponential(15.0).clip(0, 180).astype(int),
            "courses_completed": rng.poisson(4).clip(0, 50),
            "active_courses": rng.poisson(1.5).clip(0, 10),
            "quiz_completion_rate": rng.beta(8, 2).clip(0, 1),
            "forum_posts_count": rng.poisson(2).clip(0, 100),
            "events_attended": rng.poisson(1).clip(0, 20),
            "support_tickets_count": rng.poisson(0.5).clip(0, 10),
            "email_open_rate": rng.beta(3, 4).clip(0, 1),
            "email_click_rate": 0.0  # Will be calculated from open rate
        })
    
    return pd.DataFrame(data)

def get_latest_user_features(user_id):
    """
    Get the latest feature values for a specific user.
    """
    engine = get_database_engine()
    
    query = """
    WITH subscription_stats AS (
        SELECT 
            us.id,
            us.user_id,
            us.subscribed,
            us.expires,
            us.status,
            us.auto_renew,
            sp.name as plan_name,
            sp.price
        FROM usersubscriptions us
        JOIN subscriptionplans sp ON us.plan_id = sp.id
        WHERE us.user_id = :user_id
        ORDER BY us.subscribed DESC
        LIMIT 1
    ),
    invoice_stats AS (
        SELECT 
            subscription_id,
            COUNT(*) as invoice_count,
            SUM(amount) as total_spent,
            AVG(CASE WHEN paid THEN 1 ELSE 0 END) as payment_success_rate,
            SUM(CASE WHEN paid = FALSE THEN 1 ELSE 0 END) as failed_payments_count,
            MAX(issued_at) as last_payment_date
        FROM invoices
        GROUP BY subscription_id
    )
    SELECT 
        ss.id,
        ss.user_id,
        EXTRACT(DAY FROM (NOW() - ss.subscribed)) as subscription_duration_days,
        EXTRACT(DAY FROM (ss.expires - NOW())) as days_until_expiry,
        CASE 
            WHEN ss.plan_name = 'FREEMIUM' THEN 0
            WHEN ss.plan_name = 'STANDARD' THEN 1
            WHEN ss.plan_name = 'PREMIUM' THEN 2
            ELSE 0
        END as plan_tier,
        ss.auto_renew as auto_renew_enabled,
        COALESCE(istats.invoice_count, 0) as renewal_count,
        COALESCE(istats.total_spent, 0) as total_spent,
        COALESCE(istats.payment_success_rate, 1.0) as payment_success_rate,
        COALESCE(istats.failed_payments_count, 0) as failed_payments_count,
        EXTRACT(DAY FROM (NOW() - COALESCE(istats.last_payment_date, ss.subscribed))) as last_payment_days
    FROM subscription_stats ss
    LEFT JOIN invoice_stats istats ON ss.id = istats.subscription_id
    """
    
    try:
        df = pd.read_sql(query, engine, params={"user_id": user_id})
        if len(df) > 0:
            return df.iloc[0].to_dict()
        return None
    except Exception as e:
        print(f"Error fetching user features: {e}")
        return None

def save_prediction_to_database(user_id, prediction):
    """
    Save churn prediction to database for tracking and monitoring.
    """
    engine = get_database_engine()
    
    try:
        query = """
        INSERT INTO churn_predictions 
        (user_id, churn_probability, risk_level, created_at)
        VALUES (:user_id, :churn_probability, :risk_level, NOW())
        ON CONFLICT (user_id) DO UPDATE SET
            churn_probability = EXCLUDED.churn_probability,
            risk_level = EXCLUDED.risk_level,
            created_at = NOW()
        """
        
        with engine.connect() as conn:
            conn.execute(text(query), {
                "user_id": user_id,
                "churn_probability": prediction.get("churn_probability"),
                "risk_level": prediction.get("risk_level")
            })
            conn.commit()
        
        print(f"✓ Saved prediction for user {user_id}")
        return True
    except Exception as e:
        print(f"✗ Error saving prediction: {e}")
        return False

def create_churn_predictions_table():
    """
    Create the churn_predictions table if it doesn't exist.
    """
    engine = get_database_engine()
    
    create_table_query = """
    CREATE TABLE IF NOT EXISTS churn_predictions (
        id SERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE,
        churn_probability FLOAT NOT NULL,
        risk_level VARCHAR(20) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
    );
    
    CREATE INDEX IF NOT EXISTS idx_churn_user_id ON churn_predictions(user_id);
    CREATE INDEX IF NOT EXISTS idx_churn_risk_level ON churn_predictions(risk_level);
    CREATE INDEX IF NOT EXISTS idx_churn_created_at ON churn_predictions(created_at);
    """
    
    try:
        with engine.connect() as conn:
            conn.execute(text(create_table_query))
            conn.commit()
        print("✓ Created churn_predictions table")
    except Exception as e:
        print(f"✗ Error creating table: {e}")