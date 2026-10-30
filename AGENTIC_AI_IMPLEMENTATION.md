# Roamora Agentic AI Integration

## What was added

| Technology | Implementation in this project | Entry point |
|---|---|---|
| LangChain | Typed, read-only travel tools connected to existing Spring Boot APIs | `agent-service/langgraph_agent.py` |
| LangGraph | Stateful assistant graph with model/tool routing and bounded tool loops | `agent-service/langgraph_agent.py` |
| MCP | Streamable HTTP server exposing Roamora place, weather and transport tools; LangGraph discovers these tools using `langchain-mcp-adapters` | `agent-service/mcp_server.py` |
| CrewAI | Three-role research team (destination, mobility, itinerary coordinator) | `agent-service/crewai_team.py` |
| Spring Boot | Proxies existing copilot requests to the optional Python agent service and provides an authenticated CrewAI endpoint | `/api/v1/intelligence/chat`, `/api/v1/intelligence/crew-plan` |
| React | Adds a **Plan with AI team** action to Roamora Copilot | `frontend/src/components/intelligence/RoamoraCopilot.jsx` |

## Architecture

React -> authenticated Spring Boot -> Python FastAPI agent service -> LangGraph/LangChain -> MCP tools -> existing read-only Spring Boot travel APIs.

CrewAI is available as an alternate multi-agent planning endpoint. Existing direct LLM chat remains as a fallback if the optional agent sidecar is not running.

## Start locally on Windows

Start PostgreSQL and Redis first. Use three backend/AI terminals and one frontend terminal.

### Terminal 1 — MCP tool server

```powershell
cd C:\Users\srich\Downloads\Roamora\agent-service
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
$env:ROAMORA_BACKEND_URL = "http://localhost:8080"
python mcp_server.py
```

### Terminal 2 — LangGraph/LangChain + CrewAI service

```powershell
cd C:\Users\srich\Downloads\Roamora\agent-service
.\.venv\Scripts\Activate.ps1
$env:ROAMORA_AI_API_KEY = "YOUR_PROVIDER_KEY"
$env:ROAMORA_AI_MODEL = "gpt-4o-mini"
$env:ROAMORA_AI_BASE_URL = "https://api.openai.com/v1"
$env:ROAMORA_BACKEND_URL = "http://localhost:8080"
$env:ROAMORA_MCP_URL = "http://127.0.0.1:8001/mcp"
uvicorn app:app --host 127.0.0.1 --port 8090
```

### Terminal 3 — Spring Boot backend

```powershell
cd C:\Users\srich\Downloads\Roamora\backend
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
$env:ROAMORA_AI_ENABLED = "true"
$env:ROAMORA_AI_API_KEY = "YOUR_PROVIDER_KEY"
$env:ROAMORA_AI_BASE_URL = "https://api.openai.com/v1/chat/completions"
$env:ROAMORA_AI_MODEL = "gpt-4o-mini"
$env:ROAMORA_AGENT_SERVICE_ENABLED = "true"
$env:ROAMORA_AGENT_SERVICE_URL = "http://localhost:8090"
.\mvnw.cmd spring-boot:run
```

### Terminal 4 — React frontend

```powershell
cd C:\Users\srich\Downloads\Roamora\frontend
# Ensure frontend/.env.local contains:
# VITE_API_URL=http://localhost:8080/api/v1
# VITE_ROAMORA_AI_ENABLED=true
npm install
npm run dev
```

Sign in to Roamora to use the authenticated AI endpoints. In Copilot, ordinary chat uses LangGraph (which discovers MCP tools when the MCP server is available). Select **Plan with AI team** to run the CrewAI workflow.

## Verify services

- Agent service health: `http://127.0.0.1:8090/health`
- MCP Streamable HTTP: `http://127.0.0.1:8001/mcp` (use an MCP client; opening in a browser is not a meaningful tool test)
- Spring Boot health: `http://localhost:8080/api/v1/health`
- CrewAI endpoint: `POST http://localhost:8080/api/v1/intelligence/crew-plan` with the normal authenticated API session.

## Important boundaries

- The agent tools are read-only: nearby mapped places, current weather, mapped transport hubs and the indexed global transport directory.
- No booking, payment, itinerary mutation, or ticket purchase tools are exposed.
- Real-time transport data still depends on the operator's actual GTFS-Realtime or equivalent feed. MCP/agents cannot create missing live data.
- Map providers may omit photos, prices and review ratings; the agents must not invent them.
- LangGraph graph state is scoped to a request in this first implementation. The existing conversation context is passed from the frontend; persistent graph checkpointing is not configured.
- MCP binds to loopback by default. Do not expose it to the public internet without authentication, authorization, TLS, quotas, and a tool allowlist.
- CrewAI is invoked explicitly as an alternative workflow; it is not run for every chat turn.
- Keep provider API keys in environment variables. Never put them in frontend `.env`, `VITE_*` variables, source code or Git.
