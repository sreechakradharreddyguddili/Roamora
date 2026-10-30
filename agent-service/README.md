# Roamora Agent Service (LangChain, LangGraph, CrewAI and MCP)

This optional Python sidecar adds agent orchestration without replacing the existing Java/Spring Boot application.

## Components

- **LangChain** defines typed tools that call Roamora's existing read-only Spring Boot place, weather and transport APIs.
- **LangGraph** runs the stateful assistant loop: model -> conditional tool execution -> model, with a bounded recursion limit.
- **CrewAI** exposes an optional three-specialist travel planning crew at `POST /crew/plan` (destination research, mobility research, plan coordination).
- **MCP** exposes the same read-only travel tools to MCP-compatible clients over Streamable HTTP at `/mcp` on port 8001. LangGraph discovers and invokes those tools through `langchain-mcp-adapters`; if the MCP process is offline, it falls back to the same local typed LangChain tools.
- **FastAPI** provides `/health`, `/chat`, and `/crew/plan` endpoints for the Spring Boot proxy.

The tools are read-only. They do not book hotels, purchase tickets, mutate a user's itinerary, or fabricate live data. Transport schedules and vehicle positions are only available if a real operator feed exists in the backend.

## Windows setup (PowerShell)

Use Python 3.11 or 3.12 in a virtual environment. CrewAI and the AI dependencies are substantial; first installation may take several minutes.

```powershell
cd C:\Users\srich\Downloads\Roamora\agent-service
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt

$env:ROAMORA_AI_API_KEY = "YOUR_PROVIDER_KEY"
$env:ROAMORA_AI_MODEL = "gpt-4o-mini"
$env:ROAMORA_AI_BASE_URL = "https://api.openai.com/v1"
$env:ROAMORA_BACKEND_URL = "http://localhost:8080"
uvicorn app:app --host 127.0.0.1 --port 8090
```

In another terminal, start the MCP server:

```powershell
cd C:\Users\srich\Downloads\Roamora\agent-service
.\.venv\Scripts\Activate.ps1
$env:ROAMORA_BACKEND_URL = "http://localhost:8080"
python mcp_server.py
```

MCP Streamable HTTP endpoint: `http://127.0.0.1:8001/mcp`. Start this MCP process before testing LangGraph tool discovery.

## Spring Boot configuration

Set these values in the backend terminal before starting Spring Boot:

```powershell
$env:ROAMORA_AI_ENABLED = "true"
$env:ROAMORA_AGENT_SERVICE_ENABLED = "true"
$env:ROAMORA_AGENT_SERVICE_URL = "http://localhost:8090"
$env:ROAMORA_AI_API_KEY = "YOUR_PROVIDER_KEY"
$env:ROAMORA_AI_BASE_URL = "https://api.openai.com/v1/chat/completions"
$env:ROAMORA_AI_MODEL = "gpt-4o-mini"
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
.\mvnw.cmd spring-boot:run
```

The existing frontend continues to call `/api/v1/intelligence/chat`. When the agent sidecar is enabled and reachable, Spring Boot proxies chat to LangGraph. If the sidecar is unavailable, the existing direct LLM integration is used as a fallback. CrewAI is available through the authenticated Spring Boot endpoint `/api/v1/intelligence/crew-plan`.

## API examples

```http
POST http://localhost:8080/api/v1/intelligence/crew-plan
Content-Type: application/json
Authorization: Bearer <your-access-token>

{"question":"Draft a 3-day Paris itinerary","journeyContext":"Destination: Paris"}
```

## Safety and production notes

- Keep API keys in backend/agent-service environment variables, never in `VITE_*` frontend variables or source control.
- Bind MCP to localhost for development. Before exposing it remotely, add authentication, authorization, TLS, request quotas and tool-level allowlists.
- The current MCP tools are read-only and call the local Spring Boot API. They do not bypass provider coverage or create live transit feeds.
- LangGraph's state is per request in this initial integration; persistent cross-session graph checkpointing is not configured. Conversation continuity is passed from the existing frontend context.
- CrewAI is an alternative multi-agent workflow, not required for every chat. It is invoked explicitly via `/crew/plan`.
- Validate provider terms, API limits, model compatibility and dependency versions before production deployment.
