# ⚡ Quick Start Guide - Remote Database

This is a quick start guide for setting up the churn predictor with a remote PostgreSQL database.

---

## 🎯 5-Minute Setup

### Step 1: Navigate to Directory
```bash
cd abonnement/churn-predictor
```

### Step 2: Create Environment File
```bash
cp .env.example .env
```

### Step 3: Edit .env with Remote Database Credentials
```bash
nano .env  # or use your preferred editor
```

Update these lines:
```bash
# Replace with your remote database details
DB_HOST=your-database-server.com  # or IP like 192.168.1.100
DB_PORT=5432
DB_NAME=abonnements
DB_USER=your_database_user
DB_PASSWORD=your_secure_password

# Keep these as-is for now
API_HOST=0.0.0.0
API_PORT=5000
API_DEBUG=True
```

### Step 4: Test Database Connection
```bash
python test_db_connection.py
```

**Expected Output:**
```
============================================================
DATABASE CONNECTION TEST
============================================================

Configuration:
  Host: your-database-server.com
  Port: 5432
  Database: abonnements
  User: your_database_user
  Password: **************

Attempting to connect...
✓ Connection successful!

Database Version:
  PostgreSQL 15.x

Checking tables...
  Total tables found: 5
  Required tables:
    ✓ usersubscriptions
    ✓ subscriptionplans
    ✓ invoices

✓ ALL TESTS PASSED!

You can now train models with: python train_model.py
Or start the API with: python app.py
```

**If connection fails**, see [REMOTE_DATABASE_SETUP.md](REMOTE_DATABASE_SETUP.md) for troubleshooting.

### Step 5: Install Dependencies (First Time Only)
```bash
pip install -r requirements.txt
```

### Step 6: Train Models
```bash
python train_model.py
```

This will:
- Fetch real data from your remote database
- Train XGBoost and Logistic Regression models
- Save models to `models/` directory
- Create `churn_predictions` table in database

### Step 7: Start the API
```bash
python app.py
```

**Output:**
```
============================================================
CHURN PREDICTION API
============================================================
✓ Loaded XGBoost model
✓ Initialized SHAP explainer
✓ Loaded model metadata (trained at: 2026-04-10T00:00:00)
============================================================
API Endpoints:
  GET  /health            - Health check
  GET  /model-info        - Model metadata
  POST /predict           - Single user prediction
  POST /predict-batch     - Batch predictions
  GET  /predict-history   - User churn trend
============================================================
Starting server on 0.0.0.0:5000
============================================================
 * Running on http://0.0.0.0:5000
```

### Step 8: Test the API
In a new terminal:
```bash
curl http://localhost:5000/health
```

**Expected Response:**
```json
{
  "status": "ok",
  "model_loaded": true,
  "features_count": 21,
  "timestamp": "2026-04-10T00:00:00"
}
```

---

## 🎉 Done!

Your churn predictor is now running with real data from your remote PostgreSQL database!

---

## 📊 Next Steps

### Test a Prediction
```bash
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "features": {
      "login_frequency": 2.5,
      "last_login_days": 10,
      "payment_success_rate": 0.95,
      "subscription_duration_days": 180
    },
    "includeExplanations": true
  }'
```

### Integrate with Your Backend
See [README.md](README.md) for integration examples with Spring Boot.

### Monitor Predictions
Check the `churn_predictions` table in your database:
```sql
SELECT * FROM churn_predictions ORDER BY created_at DESC LIMIT 10;
```

---

## 🔧 Common Connection Scenarios

### Scenario 1: Direct Connection (Easiest)
Your database is publicly accessible or on the same network.

```bash
# .env
DB_HOST=database.yourcompany.com
DB_PORT=5432
```

**Test connection:**
```bash
python test_db_connection.py
```

### Scenario 2: Via VPN
Database is on a private network requiring VPN.

```bash
# 1. Connect to VPN
sudo openvpn --config company-vpn.ovpn

# 2. Test connectivity
ping 192.168.10.50
telnet 192.168.10.50 5432

# 3. Configure .env
DB_HOST=192.168.10.50
DB_PORT=5432
```

### Scenario 3: Via SSH Tunnel
Direct access is blocked, but SSH is available.

```bash
# Terminal 1: Create SSH tunnel
ssh -L 5433:localhost:5432 your_username@your-server.com

# Terminal 2: Configure .env to use tunnel
DB_HOST=localhost
DB_PORT=5433  # Local tunnel port

# Terminal 3: Test connection
python test_db_connection.py
```

### Scenario 4: SSL Required
Database server requires SSL/TLS connections.

```bash
# Add SSL mode to connection string in database_utils.py
# DATABASE_URL = f"postgresql://{...}?sslmode=require"
```

See [REMOTE_DATABASE_SETUP.md](REMOTE_DATABASE_SETUP.md) for detailed SSL configuration.

---

## ❓ Quick FAQ

**Q: How do I know if I need VPN or SSH?**
A: Try direct connection first with `python test_db_connection.py`. If it fails with "connection refused" or "timeout", you likely need VPN or SSH tunnel.

**Q: What if I don't have database credentials?**
A: Contact your database administrator for the credentials (host, port, username, password, database name).

**Q: Can I test without the database?**
A: Yes! If the database connection fails, the system will automatically fall back to synthetic data. You can still train models and test the API.

**Q: How do I know which data is being used?**
A: The training script will print:
- "✓ Fetched X real subscription records from database" (using real data)
- "⚠ Not enough real data available. Using synthetic data." (using synthetic)

**Q: What if the connection is slow?**
A: You can increase the timeout in `database_utils.py`:
```python
engine = create_engine(DATABASE_URL, connect_args={'connect_timeout': 30})
```

---

## 🚀 Production Deployment

For production deployment on the same server as the database:

1. Upload files to server
2. Configure `.env` with `DB_HOST=localhost`
3. Install dependencies
4. Train models
5. Set up as a systemd service

See [REMOTE_DATABASE_SETUP.md](REMOTE_DATABASE_SETUP.md) for detailed production deployment instructions.

---

## 📚 Need More Help?

- [README.md](README.md) - Full documentation
- [REMOTE_DATABASE_SETUP.md](REMOTE_DATABASE_SETUP.md) - Detailed remote database setup
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Implementation overview

---

**Last Updated**: April 10, 2026