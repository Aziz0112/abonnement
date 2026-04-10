# 🔥 CONNECTION ERROR FIXED

## ❌ Your Error
```
Failed to load resource: net::ERR_CONNECTION_REFUSED
localhost:8085/api/abonnements/churn/predict
```

## ✅ What's Wrong

Your **churn predictor API is not running**. The frontend calls the backend (port 8085), which tries to call the churn predictor (port 5000), but nothing is listening on port 5000.

## 🚀 Quick Fix (30 seconds)

### Windows
```cmd
cd abonnement\churn-predictor
start_api.bat
```

### Linux/Mac
```bash
cd abonnement/churn-predictor
./start_api.sh
```

### Or manually
```bash
cd abonnement/churn-predictor
python app.py
```

## ✅ After Starting the API

You should see:
```
============================================================
CHURN PREDICTION API
============================================================
✓ Loaded XGBoost model
✓ Initialized SHAP explainer
============================================================
Starting server on 0.0.0.0:5000
============================================================
 * Running on http://0.0.0.0:5000
```

## 🧪 Test It Works

In a new terminal:
```bash
curl http://localhost:5000/health
```

Expected response:
```json
{
  "status": "ok",
  "model_loaded": true,
  "features_count": 21
}
```

## 🎯 Go Back to Your Frontend

Now refresh your browser and try the churn prediction again - it should work!

---

## 📊 What's Happening

```
┌─────────────┐
│  Frontend   │
│  (Angular)  │
│  Port 4200  │
└──────┬──────┘
       │
       ↓
┌─────────────┐
│   Backend   │
│ (Spring)    │
│  Port 8085  │
└──────┬──────┘
       │
       ↓
┌─────────────┐
│ Churn Pred  │ ← THIS WASN'T RUNNING
│  (Python)   │
│  Port 5000  │
└─────────────┘
```

**All three services must be running:**
1. ✅ Frontend (Angular)
2. ✅ Backend (Spring Boot)
3. ❌ Churn Predictor (Python) ← YOU NEEDED TO START THIS

---

## 🔄 Keep It Running

**Important:** The churn predictor runs in the foreground. You must:
- Keep that terminal open
- Or run it in the background
- Or set it up as a service (production)

### Option 1: Run in Background

```bash
cd abonnement/churn-predictor
nohup python app.py > api.log 2>&1 &
```

### Option 2: Run in New Terminal
Just open a new terminal window and run `python app.py` there.

### Option 3: Use Startup Script
The startup script handles everything automatically.

---

## 📋 Complete Setup Checklist

Before it works, you need:

- [x] ✅ Spring Boot backend running (port 8085)
- [ ] ❌ **Churn Predictor API running (port 5000)** ← START THIS NOW
- [x] ✅ Frontend running (port 4200)
- [ ] ⚠️  Models trained (will happen automatically on first run)
- [ ] ⚠️  Database configured (will fall back to synthetic if not)

---

## 🔧 If It Still Doesn't Work

### 1. Check Port 5000 is Free
```cmd
netstat -ano | findstr :5000  # Windows
lsof -i :5000                  # Linux/Mac
```

### 2. Kill Existing Process
```cmd
taskkill /PID <PID> /F  # Windows
kill -9 <PID>           # Linux/Mac
```

### 3. Start Fresh
```bash
cd abonnement/churn-predictor
python app.py
```

---

## 📚 Full Documentation

- [TROUBLESHOOTING_CONNECTION.md](TROUBLESHOOTING_CONNECTION.md) - Detailed troubleshooting
- [QUICK_START_REMOTE.md](QUICK_START_REMOTE.md) - Quick setup guide
- [README.md](README.md) - Full documentation

---

## 🎉 Summary

**Your churn predictor API just needed to be started!**

Once you run `python app.py` (or the startup script), everything will work.

**What to do right now:**
1. Open a new terminal
2. Run: `cd abonnement/churn-predictor`
3. Run: `python app.py`
4. Keep that terminal open
5. Go back to your frontend and try again

That's it! 🚀