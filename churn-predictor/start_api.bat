@echo off
REM Churn Predictor API Startup Script for Windows
REM Run this to start churn prediction API server

echo ============================================================
echo 🚀 STARTING CHURN PREDICTOR API
echo ============================================================

REM Check if virtual environment exists
if not exist "venv" (
    echo.
    echo ⚠️  Virtual environment not found!
    echo Creating virtual environment...
    python -m venv venv
    call venv\Scripts\activate
    pip install -r requirements.txt
) else (
    echo ✅ Virtual environment found
    call venv\Scripts\activate
)

REM Check if models exist
if not exist "models" (
    echo.
    echo ⚠️  Models not found!
    echo Training models first...
    python train_model.py
    echo.
)

REM Check if .env exists
if not exist ".env" (
    echo.
    echo ⚠️  .env file not found!
    echo Creating from .env.example...
    copy .env.example .env
    echo ⚠️  Please edit .env with your database credentials before running again!
    pause
    exit /b 1
)

REM Test database connection
echo.
echo 🔍 Testing database connection...
python test_db_connection.py

if errorlevel 1 (
    echo.
    echo ⚠️  Database connection failed!
    echo Please check your .env configuration
    pause
    exit /b 1
)

echo.
echo ============================================================
echo ✅ All checks passed! Starting API server...
echo ============================================================
echo.
echo 📡 API will be available at: http://localhost:5000
echo 📊 Health check: http://localhost:5000/health
echo 📄 API docs: See README.md
echo.
echo ⏹  Press Ctrl+C to stop the server
echo ============================================================
echo.

REM Start Flask API
python app.py

pause