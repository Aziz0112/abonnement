#!/bin/bash

# Churn Predictor API Startup Script
# Run this to start the churn prediction API server

echo "============================================================"
echo "🚀 STARTING CHURN PREDICTOR API"
echo "============================================================"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "⚠️  Virtual environment not found!"
    echo "Creating virtual environment..."
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
else
    echo "✅ Virtual environment found"
    source venv/bin/activate
fi

# Check if models exist
if [ ! -d "models" ]; then
    echo ""
    echo "⚠️  Models not found!"
    echo "Training models first..."
    python train_model.py
    echo ""
fi

# Check if .env exists
if [ ! -f ".env" ]; then
    echo ""
    echo "⚠️  .env file not found!"
    echo "Creating from .env.example..."
    cp .env.example .env
    echo "⚠️  Please edit .env with your database credentials before running again!"
    exit 1
fi

# Test database connection
echo "🔍 Testing database connection..."
python test_db_connection.py

if [ $? -ne 0 ]; then
    echo ""
    echo "⚠️  Database connection failed!"
    echo "Please check your .env configuration"
    exit 1
fi

echo ""
echo "============================================================"
echo "✅ All checks passed! Starting API server..."
echo "============================================================"
echo ""
echo "📡 API will be available at: http://localhost:5000"
echo "📊 Health check: http://localhost:5000/health"
echo "📄 API docs: See README.md"
echo ""
echo "⏹  Press Ctrl+C to stop the server"
echo "============================================================"
echo ""

# Start Flask API
python app.py