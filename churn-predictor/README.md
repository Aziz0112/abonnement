# 🚀 Enhanced Churn Prediction System

An advanced ML-powered churn prediction system for MiNoLingo educational platform with real-time predictions, SHAP explainability, and automated retention recommendations.

## ✨ What's New - Major Improvements

### 🔥 High Priority Features (Implemented)

1. **Real Database Integration**
   - Connects to PostgreSQL for real-time data
   - Automatically fetches subscription, payment, and invoice data
   - Falls back to synthetic data if insufficient real data

2. **21 Enhanced Features** (up from 9)
   - Engagement: login frequency, courses completed, forum posts, events attended
   - Payment: total spent, payment success rate, failed payments, invoice frequency
   - Subscription: plan tier, auto-renew status, renewal count, days until expiry
   - Communication: email open/click rates
   - Activity: quiz completion, support tickets
   - Incentives: discount codes used

3. **Dynamic Risk Thresholds**
   - Standard mode: HIGH > 0.7, MEDIUM > 0.3
   - Aggressive mode (campaigns): HIGH > 0.5, MEDIUM > 0.2
   - Conservative mode (high-value): HIGH > 0.8, MEDIUM > 0.4

4. **Retention Recommendations**
   - 10+ actionable recommendations per user
   - Priority levels: CRITICAL, HIGH, MEDIUM, LOW
   - Categories: support, email, incentive, feature, sales

5. **SHAP Explainability**
   - Individual prediction explanations
   - Feature contribution analysis
   - Increases/Decreases churn risk indicators

6. **Time-Based Trend Analysis**
   - Track churn probability over time
   - Trend direction (increasing/decreasing/stable)
   - Historical analysis for each user

---

## 📋 Prerequisites

- Python 3.8+
- PostgreSQL (for real data integration)
- pip package manager

---

## 🚀 Installation

### 1. Clone or Navigate to Directory

```bash
cd abonnement/churn-predictor
```

### 2. Create Virtual Environment

```bash
python -m venv venv

# Windows
venv\Scripts\activate

# macOS/Linux
source venv/bin/activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

Required packages:
- flask==3.1.0
- flask-cors==5.0.1
- scikit-learn==1.6.1
- xgboost==2.1.4
- pandas==2.2.3
- numpy==2.2.3
- joblib==1.4.2
- shap==0.43.0
- psycopg2-binary==2.9.9
- sqlalchemy==2.0.36
- schedule==1.2.2

### 4. Configure Environment

```bash
# Copy example env file
cp .env.example .env

# Edit .env with your database credentials
# DB_HOST=localhost
# DB_PORT=5432
# DB_NAME=abonnements
# DB_USER=postgres
# DB_PASSWORD=your_password
```

---

## 🎓 Training Models

### Train with Real Data (if available)

```bash
python train_model.py
```

This will:
1. Attempt to fetch real data from PostgreSQL
2. If insufficient data, use enhanced synthetic data (5000 samples)
3. Train both Logistic Regression and XGBoost models
4. Save models to `models/` directory
5. Create `churn_predictions` table in database
6. Generate feature importance rankings
7. Save model metadata

### Training Output

```
============================================================
CHURN PREDICTION MODEL TRAINING
============================================================
✓ Fetched 1500 real subscription records from database
✓ Using 1500 real records from database

Features used (21):
  1. login_frequency
  2. last_login_days
  3. courses_completed
  ...

Target distribution:
  Not churned (0): 1200 (80.0%)
  Churned (1): 300 (20.0%)

============================================================
XGBOOST
============================================================
Classification Report:
              precision    recall  f1-score   support

Not Churned       0.92      0.95      0.93       240
    Churned       0.75      0.63      0.69        60

    accuracy                           0.89       300
   macro avg       0.84      0.79      0.81       300
weighted avg       0.89      0.89      0.89       300

ROC-AUC: 0.9234

============================================================
FEATURE IMPORTANCE (XGBOOST)
============================================================
  1. payment_success_rate         0.2345
  2. subscription_duration_days   0.1876
  3. email_open_rate            0.1567
  ...

============================================================
✓ TRAINING COMPLETE!
============================================================
```

---

## 🌐 Running the API

### Start Flask Server

```bash
python app.py
```

Server will start on `http://0.0.0.0:5000` by default.

### API Endpoints

#### 1. Health Check

```bash
GET /health
```

**Response:**
```json
{
  "status": "ok",
  "model_loaded": true,
  "features_count": 21,
  "timestamp": "2026-04-10T00:00:00"
}
```

#### 2. Model Information

```bash
GET /model-info
```

**Response:**
```json
{
  "model_type": "XGBClassifier",
  "feature_names": ["login_frequency", "last_login_days", ...],
  "n_features": 21,
  "n_samples": 5000,
  "churn_rate": 0.2,
  "high_risk_threshold": 0.7,
  "medium_risk_threshold": 0.3,
  "roc_auc": 0.9234,
  "trained_at": "2026-04-10T00:00:00"
}
```

#### 3. Single User Prediction (with recommendations & SHAP)

```bash
POST /predict
Content-Type: application/json

{
  "userId": 123,
  "features": {
    "login_frequency": 2.5,
    "last_login_days": 10,
    "courses_completed": 5,
    "active_courses": 2,
    "payment_success_rate": 0.95,
    "failed_payments_count": 0,
    "total_spent": 150.0,
    "last_payment_days": 5,
    "subscription_duration_days": 180,
    "days_until_expiry": 30,
    "plan_tier": 1,
    "auto_renew_enabled": true,
    "renewal_count": 2,
    "email_open_rate": 0.45,
    "email_click_rate": 0.25,
    "quiz_completion_rate": 0.80,
    "forum_posts_count": 10,
    "events_attended": 3,
    "support_tickets_count": 1,
    "discount_codes_used": 1,
    "invoice_frequency": 30
  },
  "context": {
    "urgent_retention": false
  },
  "include_explanations": true
}
```

**Response:**
```json
{
  "userId": 123,
  "churn_probability": 0.1234,
  "risk_level": "LOW",
  "timestamp": "2026-04-10T00:00:00",
  "feature_importance": {
    "payment_success_rate": 0.2345,
    "subscription_duration_days": 0.1876,
    ...
  },
  "shap_explanation": [
    {
      "feature": "payment_success_rate",
      "contribution": -0.2345,
      "value": 0.95,
      "impact": "decreases"
    },
    {
      "feature": "subscription_duration_days",
      "contribution": -0.1234,
      "value": 180.0,
      "impact": "decreases"
    },
    ...
  ],
  "recommendations": [
    {
      "priority": "MEDIUM",
      "action": "Low login frequency. Consider gamification or progress reminders.",
      "type": "feature"
    },
    {
      "priority": "MEDIUM",
      "action": "User hasn't completed any courses. Send onboarding guide.",
      "type": "feature"
    }
  ],
  "narrative": "This user has a low risk of churning (probability: 12.3%).\n\nKey factors:\n• Good payment_success_rate (0.95) reduces churn risk\n• Good subscription_duration_days (180.00) reduces churn risk\n\n2 retention actions available."
}
```

#### 4. Batch Predictions

```bash
POST /predict-batch
Content-Type: application/json

{
  "users": [
    {
      "userId": 1,
      "features": { "login_frequency": 1.5, "last_login_days": 45, ... }
    },
    {
      "userId": 2,
      "features": { "login_frequency": 4.0, "last_login_days": 2, ... }
    }
  ],
  "context": {
    "urgent_retention": true
  }
}
```

**Response:**
```json
{
  "predictions": [
    {
      "userId": 1,
      "churn_probability": 0.7890,
      "risk_level": "HIGH"
    },
    {
      "userId": 2,
      "churn_probability": 0.0234,
      "risk_level": "LOW"
    }
  ],
  "feature_importance": { ... },
  "timestamp": "2026-04-10T00:00:00",
  "total_users": 2
}
```

#### 5. Churn History/Trend

```bash
GET /predict-history?userId=123&days=30
```

**Response:**
```json
{
  "userId": 123,
  "trend": [
    {
      "date": "2026-03-10",
      "churn_probability": 0.1500,
      "risk_level": "LOW"
    },
    {
      "date": "2026-03-15",
      "churn_probability": 0.1800,
      "risk_level": "MEDIUM"
    },
    ...
  ],
  "trend_direction": "increasing",
  "period_days": 30,
  "current_probability": 0.2500,
  "timestamp": "2026-04-10T00:00:00"
}
```

---

## 📊 Features Explained

### Engagement Features
- **login_frequency**: Average logins per week (0-30)
- **last_login_days**: Days since last login (0-180)
- **courses_completed**: Number of completed courses (0-50)
- **active_courses**: Currently enrolled courses (0-10)
- **quiz_completion_rate**: Percentage of quizzes completed (0-1)
- **forum_posts_count**: Number of forum posts (0-100)
- **events_attended**: Events participated in (0-20)

### Payment Features
- **payment_success_rate**: Payment success rate (0-1)
- **failed_payments_count**: Failed payment attempts (0-10)
- **total_spent**: Total money spent ($)
- **last_payment_days**: Days since last payment (0-180)
- **invoice_frequency**: Average days between invoices (7-90)

### Subscription Features
- **subscription_duration_days**: Days as subscriber (1-730)
- **days_until_expiry**: Days until subscription expires (-30 to 180)
- **plan_tier**: Plan tier (0=FREEMIUM, 1=STANDARD, 2=PREMIUM)
- **auto_renew_enabled**: Auto-renew enabled (0 or 1)
- **renewal_count**: Number of renewals (0-10)

### Communication Features
- **email_open_rate**: Email open rate (0-1)
- **email_click_rate**: Email click rate (0-1)

### Activity Features
- **support_tickets_count**: Support tickets opened (0-10)

### Incentive Features
- **discount_codes_used**: Number of discount codes used (0-5)

---

## 🎯 Risk Levels

### LOW (0-30%)
- User is stable and unlikely to churn
- Monitor but no urgent action needed

### MEDIUM (30-70%)
- User shows some churn indicators
- Send engagement emails and offers
- Monitor closely

### HIGH (70-100%)
- User at significant risk of churning
- Immediate outreach needed
- Consider retention offers

---

## 💡 Retention Recommendations

The system provides prioritized, actionable recommendations:

### Categories
- **support**: Direct customer support intervention
- **email**: Email campaigns and reminders
- **incentive**: Discounts and offers
- **feature**: Product feature improvements
- **sales**: Upgrade opportunities
- **email_strategy**: Email optimization

### Priority Levels
- **CRITICAL**: Immediate action required
- **HIGH**: Action within 24-48 hours
- **MEDIUM**: Action within 1 week
- **LOW**: Monitor and consider

---

## 🔍 SHAP Explainability

SHAP (SHapley Additive exPlanations) provides:
- Individual prediction explanations
- Feature contribution values
- Increases/Decreases risk indicators

**Example SHAP output:**
```json
{
  "feature": "payment_success_rate",
  "contribution": -0.2345,
  "value": 0.95,
  "impact": "decreases"
}
```

This means:
- Feature: `payment_success_rate`
- User's value: 0.95 (95%)
- Contribution: -0.2345 (reduces churn risk)
- Impact: "decreases" (positive impact on retention)

---

## 📈 Model Performance

### Expected Metrics
- **Accuracy**: 85-95%
- **Precision**: 75-90% (for churned users)
- **Recall**: 60-80% (for churned users)
- **ROC-AUC**: 0.85-0.95

### Feature Importance (Typical)
1. Payment success rate (~20-25%)
2. Subscription duration (~15-20%)
3. Email open rate (~10-15%)
4. Days until expiry (~8-12%)
5. Plan tier (~5-10%)

---

## 🔄 Integration with MiNoLingo Backend

### Database Schema

```sql
-- Automatically created by train_model.py
CREATE TABLE churn_predictions (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    churn_probability FLOAT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Backend Integration Example

```java
// In ChurnPredictorController.java
@RestController
@RequestMapping("/api/abonnements/churn")
public class ChurnPredictorController {
    
    private final RestTemplate restTemplate;
    private final String CHURN_API_URL = "http://localhost:5000";
    
    @PostMapping("/predict/{userId}")
    public ResponseEntity<?> predictChurn(@PathVariable Long userId) {
        // Fetch user features
        Map<String, Object> features = getUserFeatures(userId);
        
        // Call churn predictor
        Map<String, Object> request = Map.of(
            "userId", userId,
            "features", features
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            CHURN_API_URL + "/predict",
            request,
            Map.class
        );
        
        return ResponseEntity.ok(response.getBody());
    }
}
```

---

## 🛠️ Troubleshooting

### Database Connection Issues

```
✗ Error fetching data from database: connection refused
```

**Solution:**
- Check PostgreSQL is running
- Verify credentials in `.env`
- Check database name and host

### Model Not Found

```
FileNotFoundError: No trained model found in models/
```

**Solution:**
```bash
python train_model.py
```

### SHAP Explainer Error

```
⚠ Could not initialize SHAP explainer
```

**Solution:**
- API will still work without SHAP
- Install: `pip install shap`
- Ensure using XGBoost model (not Logistic Regression)

---

## 📝 Future Enhancements

- [ ] Real-time scoring via WebSocket
- [ ] A/B testing framework for retention strategies
- [ ] Customer Lifetime Value (CLV) prediction
- [ ] Segmented models (new users vs long-term)
- [ ] Automated model retraining
- [ ] Anomaly detection
- [ ] Performance monitoring dashboard
- [ ] Natural language insights
- [ ] Integration with user engagement microservice

---

## 📞 Support

For issues or questions:
1. Check this README
2. Review error logs
3. Verify database connection
4. Ensure models are trained

---

## 📄 License

Internal project for MiNoLingo platform.

---

## 🎉 Summary

This enhanced churn predictor provides:

✅ **Real data integration** with PostgreSQL
✅ **21 predictive features** across engagement, payment, and subscription
✅ **Dynamic risk thresholds** for different business contexts
✅ **Actionable retention recommendations** with priorities
✅ **SHAP explainability** for individual predictions
✅ **Time-based trend analysis** for historical tracking
✅ **Batch prediction support** for multiple users
✅ **Database persistence** for prediction tracking
✅ **Comprehensive API** with 5 endpoints

Start predicting churn and improving retention today! 🚀