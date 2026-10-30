from __future__ import annotations

import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from langgraph_agent import run_graph
from crewai_team import run_crew_plan


class ChatRequest(BaseModel):
    question: str = Field(min_length=1, max_length=1200)
    journeyContext: Optional[str] = Field(default=None, max_length=12000)


class ChatResponse(BaseModel):
    answer: str
    framework: str
    toolsAvailable: list[str]


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="Roamora Agent Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "ok", "service": "roamora-agent-service", "llmConfigured": bool(os.getenv("ROAMORA_AI_API_KEY", "").strip())}


@app.post("/chat", response_model=ChatResponse)
async def chat(body: ChatRequest):
    try:
        answer = await run_graph(body.question, body.journeyContext or "")
        return ChatResponse(answer=answer, framework="LangGraph + LangChain", toolsAvailable=["nearby_places", "current_weather", "transport_hubs", "global_transport_directory"])
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Roamora agent workflow failed: {str(exc)[:500]}") from exc


@app.post("/crew/plan", response_model=ChatResponse)
def crew_plan(body: ChatRequest):
    try:
        answer = run_crew_plan(body.question, body.journeyContext or "")
        return ChatResponse(answer=answer, framework="CrewAI", toolsAvailable=["crew_search_places", "crew_search_transport"])
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Roamora CrewAI workflow failed: {str(exc)[:500]}") from exc
