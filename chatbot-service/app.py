import os
import requests
from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List

app = FastAPI(title="MiNoLingo Analytics Chatbot Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

API_KEY = os.getenv("CHATBOT_API_KEY", "minolingo-chatbot-secret-2026")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b")


# ── Models ──────────────────────────────────────────────────────────────────

class MonthlyRevenue(BaseModel):
    year: int
    month: int
    revenue: float

class MonthlyGrowth(BaseModel):
    year: int
    month: int
    newSubscribers: int

class AnalyticsDashboard(BaseModel):
    totalSubscribers: int = 0
    activeSubscribers: int = 0
    expiredSubscribers: int = 0
    cancelledSubscribers: int = 0
    monthlyRevenue: List[MonthlyRevenue] = []
    monthlyGrowth: List[MonthlyGrowth] = []
    growthRatePercent: float = 0.0

class ChatRequest(BaseModel):
    message: str
    analytics: Optional[AnalyticsDashboard] = None
    userId: Optional[int] = None

class ChatResponse(BaseModel):
    message: str
    suggestions: List[str] = []


# ── Helpers ──────────────────────────────────────────────────────────────────

def verify_key(x_api_key: str):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=403, detail="Invalid API key")


def build_system_prompt(request: ChatRequest) -> str:
    prompt = (
        "You are an AI analytics assistant for MinoLingo, "
        "an education platform for children to learn English and other languages.\n\n"
    )

    if request.analytics:
        a = request.analytics
        prompt += "Current Analytics Data:\n"
        prompt += f"- Total Subscribers: {a.totalSubscribers}\n"
        prompt += f"- Active Subscribers: {a.activeSubscribers}\n"
        prompt += f"- Expired Subscriptions: {a.expiredSubscribers}\n"
        prompt += f"- Cancelled Subscriptions: {a.cancelledSubscribers}\n"
        prompt += f"- Growth Rate: {a.growthRatePercent}%\n"

        if a.monthlyRevenue:
            latest = a.monthlyRevenue[-1]
            prompt += f"- Latest Monthly Revenue: ${latest.revenue:.2f} ({latest.month}/{latest.year})\n"

        if a.monthlyGrowth:
            latest_g = a.monthlyGrowth[-1]
            prompt += f"- Latest Monthly New Subscribers: {latest_g.newSubscribers} ({latest_g.month}/{latest_g.year})\n"

        prompt += "\n"

    prompt += (
        "Guidelines:\n"
        "1. Be professional, concise, and data-driven\n"
        "2. Use specific numbers from the analytics data when answering\n"
        "3. Provide actionable recommendations when asked\n"
        "4. Keep responses between 80-200 words\n"
        "5. Always stay in context of the MinoLingo subscription platform\n"
    )
    return prompt


def generate_suggestions(message: str) -> List[str]:
    msg = message.lower()
    if any(w in msg for w in ["revenue", "money", "income"]):
        return ["Show revenue by month", "Compare with last month", "Revenue growth analysis"]
    if any(w in msg for w in ["subscriber", "user", "member"]):
        return ["Subscriber growth trend", "Active vs inactive ratio", "Churn insights"]
    if any(w in msg for w in ["growth", "trend", "rate"]):
        return ["Monthly growth breakdown", "Growth rate analysis", "Future projections"]
    if any(w in msg for w in ["recommend", "improve", "strategy"]):
        return ["Retention strategies", "Growth tips", "Reduce churn ideas"]
    return ["How much revenue this month?", "Subscriber growth trend", "Recommend improvements"]


def call_ollama(system_prompt: str, user_message: str) -> str:
    payload = {
        "model": OLLAMA_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message},
        ],
        "stream": False,
    }

    try:
        response = requests.post(OLLAMA_URL, json=payload, timeout=120)
        response.raise_for_status()
        data = response.json()

        # Native Ollama format: {"message": {"role": "assistant", "content": "..."}}
        if "message" in data and "content" in data["message"]:
            return data["message"]["content"]

        # OpenAI-compatible format: {"choices": [{"message": {"content": "..."}}]}
        if "choices" in data and data["choices"]:
            return data["choices"][0]["message"]["content"]

        return "I could not generate a response. Please try again."

    except requests.exceptions.Timeout:
        return "The AI model is taking too long. Please try again."
    except requests.exceptions.ConnectionError:
        return "Could not connect to the AI model. Please check Ollama is running."
    except Exception as e:
        return f"An error occurred: {str(e)}"


# ── Endpoints ─────────────────────────────────────────────────────────────────

@app.get("/")
def root():
    return {"service": "chatbot", "status": "running", "model": OLLAMA_MODEL}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest, x_api_key: str = Header(...)):
    verify_key(x_api_key)

    system_prompt = build_system_prompt(request)
    ai_message = call_ollama(system_prompt, request.message)
    suggestions = generate_suggestions(request.message)

    return ChatResponse(message=ai_message, suggestions=suggestions)
