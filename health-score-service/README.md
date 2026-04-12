# Health Score Service

FastAPI microservice that computes a subscription platform health score (0–100, grade A–D) from analytics data provided by the Spring Boot backend.

## Setup

```bash
cd health-score-service
python -m venv venv
venv\Scripts\activate       # Windows
pip install -r requirements.txt
```

## Run

```bash
uvicorn app:app --host 0.0.0.0 --port 8086 --reload
```

## API

### POST /health-score

**Headers:** `X-Api-Key: minolingo-health-secret-2026`

**Body:**
```json
{
  "totalSubscribers": 100,
  "activeSubscribers": 75,
  "expiredSubscribers": 15,
  "cancelledSubscribers": 10,
  "growthRatePercent": 15.5
}
```

**Response:**
```json
{
  "healthScore": 72,
  "grade": "B",
  "status": "Stable",
  "color": "blue",
  "alerts": ["All metrics within healthy range"],
  "breakdown": { ... }
}
```

## Environment variables

- `HEALTH_API_KEY` — override the default API key
