from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import os

app = FastAPI(title="MiNoLingo Health Score Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

API_KEY = os.getenv("HEALTH_API_KEY", "minolingo-health-secret-2026")


class AnalyticsInput(BaseModel):
    totalSubscribers: int
    activeSubscribers: int
    expiredSubscribers: int
    cancelledSubscribers: int
    growthRatePercent: float


def verify_key(x_api_key: str):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API key")


@app.get("/")
def root():
    return {"service": "health-score", "status": "running"}


@app.post("/health-score")
def health_score(data: AnalyticsInput, x_api_key: str = Header(...)):
    verify_key(x_api_key)

    total = data.totalSubscribers
    active = data.activeSubscribers
    cancelled = data.cancelledSubscribers
    expired = data.expiredSubscribers
    growth = data.growthRatePercent

    active_ratio = active / total if total > 0 else 0
    cancel_ratio = cancelled / total if total > 0 else 0
    expired_ratio = expired / total if total > 0 else 0

    active_points = active_ratio * 60
    growth_points = max(-20, min(30, growth * 1.5))
    cancel_penalty = cancel_ratio * 30
    expired_penalty = expired_ratio * 15

    score = active_points + growth_points - cancel_penalty - expired_penalty
    score = round(max(0, min(100, score)))

    if score >= 75:
        grade, status, color = "A", "Healthy", "green"
    elif score >= 55:
        grade, status, color = "B", "Stable", "blue"
    elif score >= 35:
        grade, status, color = "C", "Needs Attention", "orange"
    else:
        grade, status, color = "D", "Critical", "red"

    alerts = []
    if cancel_ratio > 0.2:
        alerts.append("High cancellation rate")
    if growth < 0:
        alerts.append("Negative growth trend")
    if active_ratio < 0.4 and total > 0:
        alerts.append("Low active subscriber ratio")
    if not alerts:
        alerts.append("All metrics within healthy range")

    return {
        "healthScore": score,
        "grade": grade,
        "status": status,
        "color": color,
        "alerts": alerts,
        "breakdown": {
            "activeContribution": round(active_points, 1),
            "growthContribution": round(growth_points, 1),
            "cancelPenalty": round(cancel_penalty, 1),
            "expiredPenalty": round(expired_penalty, 1),
        },
    }
