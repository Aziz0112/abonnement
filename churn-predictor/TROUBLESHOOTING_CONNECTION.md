# 🔧 Churn Predictor Connection Troubleshooting Guide

This guide helps you resolve common connection issues with the churn predictor API.

---

## ❌ Common Error: `ERR_CONNECTION_REFUSED`

### What This Means
Your frontend is trying to connect to the churn predictor API, but the API is not running or is not accessible.

### Quick Fix: Start the Churn Predictor API

#### Windows
```cmd
cd abonnement\churn-predictor
start_api.bat
```

#### Linux/Mac
```bash
cd abonnement/churn-predictor
./start_api.sh
```

#### Manual Start (Any OS)
```bash
cd abonnement/churn-predictor
python app.py
```

---

## 🔍 Diagnosis Steps

### Step 1: Check if Churn Predictor is Running

**Windows:**
```cmd
netstat -ano | findstr :5000
```

**Linux/Mac:**
```bash
lsof -i :5000
```

**Expected Output:** You should see a Python process listening on port 5000.

**No output?** The API is not running. Start it using the commands above.

---

### Step 2: Test API Directly

Open a new terminal and try:

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

**Error?** See specific error solutions below.

---

## 🐛 Specific Error Solutions

### Error 1: `ERR_CONNECTION_REFUSED` on `localhost:5000`

**Cause:** Churn predictor API is not running.

**Solution:**
```bash
cd abonnement/churn-predictor
python app.py
```

---

### Error 2: `Address already in use` on port 5000

**Cause:** Another application is using port 5000.

**Solution 1: Kill the existing process**

**Windows:**
```cmd
netstat -ano | findstr :5000
taskkill /PID <PID> /F
```

**Linux/Mac:**
```bash
lsof -ti :5000 | xargs kill -9
```

**Solution 2: Use a different port**

Edit `.env`:
```bash
API_PORT=5001
```

Then update backend `application.properties`:
```properties
churn.predictor.url=http://localhost:5001
```

---

### Error 3: `Connection timeout`

**Cause:** Firewall blocking connection or network issues.

**Solution:**
- Check if port 5000 is open in firewall
- Disable firewall temporarily for testing
- Check if VPN is required

**Windows Firewall:**
```cmd
netsh advfirewall firewall add rule name="Allow Port 5000" dir=in action=allow protocol=TCP localport=5000
```

---

### Error 4: `Model not found`

**Cause:** Models haven't been trained yet.

**Solution:**
```bash
cd abonnement/churn-predictor
python train_model.py
```

---

### Error 5: `Database connection failed`

**Cause:** Database credentials wrong or database not accessible.

**Solution:**
```bash
cd abonnement/churn-predictor
python test_db_connection.py
```

Follow the troubleshooting guide in the output.

---

### Error 6: `ImportError: No module named 'flask'`

**Cause:** Dependencies not installed.

**Solution:**
```bash
cd abonnement/churn-predictor
pip install -r requirements.txt
```

---

## 🔄 Complete Restart Procedure

If nothing works, do a complete restart:

### Windows
```cmd
REM Stop existing processes
netstat -ano | findstr :5000
taskkill /PID <PID> /F

REM Navigate and restart
cd abonnement\churn-predictor
python app.py
```

### Linux/Mac
```bash
# Stop existing processes
lsof -ti :5000 | xargs kill -9

# Navigate and restart
cd abonnement/churn-predictor
python app.py
```

---

## 📊 Architecture Overview

```
┌─────────────────┐
│   Frontend      │
│   (Angular)     │
│   Port: 4200    │
└────────┬────────┘
         │ HTTP
         ↓
┌─────────────────┐
│  Spring Boot    │
│  Backend        │
│  Port: 8085     │
└────────┬────────┘
         │ HTTP
         ↓
┌─────────────────┐
│ Churn Predictor │
│   (Python)      │
│   Port: 5000    │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ PostgreSQL DB   │
│   Port: 5432    │
└─────────────────┘
```

**All three services must be running:**
1. ✅ Frontend (Angular) - `ng serve`
2. ✅ Backend (Spring Boot) - `mvn spring-boot:run`
3. ✅ Churn Predictor (Python) - `python app.py` ← THIS IS YOUR ISSUE

---

## 🚀 Running All Services

### Terminal 1: Start Spring Boot Backend
```bash
cd abonnement
mvn spring-boot:run
```

### Terminal 2: Start Churn Predictor
```bash
cd abonnement/churn-predictor
python app.py
```

### Terminal 3: Start Frontend
```bash
cd frontend
ng serve
```

### Terminal 4: Test Connection
```bash
curl http://localhost:5000/health
curl http://localhost:8085/api/abonnements/churn/health
```

---

## 🔍 Debugging Checklist

Before reporting an issue, check:

- [ ] Churn predictor API is running (`python app.py`)
- [ ] API is on port 5000 (check with `netstat` or `lsof`)
- [ ] Can access `http://localhost:5000/health` in browser
- [ ] Spring Boot backend is running
- [ ] Backend configuration points to correct URL (`http://localhost:5000`)
- [ ] No firewall blocking port 5000
- [ ] Models exist in `models/` directory
- [ ] Database connection works (`python test_db_connection.py`)

---

## 📝 Common Configuration Issues

### Issue 1: Wrong Port in Backend

**Symptom:** Frontend calls backend, backend calls wrong URL

**Check:** `abonnement/src/main/resources/application.properties`
```properties
churn.predictor.url=http://localhost:5000  # Should be 5000
```

### Issue 2: Different Port in Churn Predictor

**Symptom:** Churn predictor on different port than expected

**Check:** `abonnement/churn-predictor/.env`
```bash
API_PORT=5000  # Should match backend config
```

### Issue 3: Churn Predictor on Remote Server

**Symptom:** Backend calls localhost but churn predictor is remote

**Solution:** Update backend configuration:
```properties
churn.predictor.url=http://remote-server.com:5000
```

---

## 🆘 Still Having Issues?

### Collect Diagnostic Information

Run these commands and save the output:

```bash
# 1. Check running processes
netstat -ano | findstr :5000  # Windows
lsof -i :5000                  # Linux/Mac

# 2. Test API health
curl http://localhost:5000/health

# 3. Test backend health
curl http://localhost:8085/api/abonnements/churn/health

# 4. Check database connection
cd abonnement/churn-predictor
python test_db_connection.py

# 5. Check logs
# Look for error messages in all terminals
```

### What to Report

When asking for help, include:
1. Operating system (Windows/Linux/Mac)
2. Python version (`python --version`)
3. Error message (exact text)
4. Output of diagnostic commands above
5. Screenshot of browser console (F12)
6. Terminal output from all services

---

## 🎯 Quick Reference

| Problem | Solution |
|---------|----------|
| ERR_CONNECTION_REFUSED | Start churn predictor: `python app.py` |
| Address already in use | Kill process or change port |
| Connection timeout | Check firewall/VPN |
| Model not found | Train models: `python train_model.py` |
| Database error | Test connection: `python test_db_connection.py` |
| Import errors | Install dependencies: `pip install -r requirements.txt` |

---

## 📚 Additional Resources

- [README.md](README.md) - Full documentation
- [QUICK_START_REMOTE.md](QUICK_START_REMOTE.md) - Quick setup guide
- [REMOTE_DATABASE_SETUP.md](REMOTE_DATABASE_SETUP.md) - Database configuration
- [test_db_connection.py](test_db_connection.py) - Database test script

---

**Last Updated:** April 10, 2026