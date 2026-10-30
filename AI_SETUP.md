# Roamora AI Copilot setup

Roamora's Copilot has two layers:

1. **Local app actions** work without an LLM key. The Copilot can open the planner, prefill an origin/destination, and start mapped searches for stays, restaurants and cafés.
2. **LLM answers** use the existing Spring Boot `/api/v1/intelligence/chat` endpoint. This requires an OpenAI-compatible provider, a valid server-side API key and an authenticated user session.

The API key must remain on the backend. Never put it in `frontend/.env`, a `VITE_*` variable, source code, a public repository, or a message to the assistant.

## Local Windows PowerShell setup

Start the backend in `Roamora\backend` after setting these values in the same PowerShell terminal:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=UTC"
$env:ROAMORA_AI_ENABLED = "true"
$env:ROAMORA_AI_API_KEY = "PASTE_YOUR_PROVIDER_KEY_HERE"
$env:ROAMORA_AI_BASE_URL = "https://api.openai.com/v1/chat/completions"
$env:ROAMORA_AI_MODEL = "gpt-4o-mini"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/weatherwise"
$env:DATABASE_USERNAME = "weatherwise"
$env:DATABASE_PASSWORD = "weatherwise_dev_password"
.\mvnw.cmd spring-boot:run
```

Replace `PASTE_YOUR_PROVIDER_KEY_HERE` locally. Do not commit the key. If you use a different OpenAI-compatible provider, use that provider's documented base URL and model name.

In a second terminal, copy `frontend/.env.example` to `frontend/.env.local`, then set:

```dotenv
VITE_API_URL=http://localhost:8080/api/v1
VITE_ROAMORA_AI_ENABLED=true
```

The frontend intentionally defaults to local, fast travel-aware responses when this flag is absent or `false`; this avoids making every chat message wait for a missing AI provider. Set it to `true` only after the backend provider is configured. The API key belongs only in the backend terminal/environment. The AI endpoint has a bounded provider timeout and the UI falls back to local guidance if the provider is unavailable.

Restart Vite after changing frontend environment variables:

```powershell
cd ..\frontend
npm install
npm run dev
```

## Current capability boundaries

- Deterministic app actions are implemented for common navigation and place-search requests.
- General conversational answers are provided by the configured LLM when it is enabled and the user is signed in.
- Route, weather and nearby-place results must still come from the app's connected providers; the model should not invent missing route availability, hotel prices, ratings or weather.
- This update does **not** claim a full vector-database RAG pipeline or general-purpose LangGraph workflow. Those should only be added after Roamora has a curated travel knowledge corpus, retrieval evaluation, explicit tool schemas, rate limiting and safety tests.
- Production launch still requires HTTPS, production database/Redis settings, a restricted CORS allowlist, a strong JWT secret, API quotas/rate limits and end-to-end provider tests.

## Agentic AI upgrade (optional)

Roamora now includes an optional `agent-service/` Python sidecar that adds:

- LangChain typed tools for the existing Roamora places, weather and transport APIs.
- LangGraph stateful tool-calling orchestration for the normal Copilot chat.
- An MCP Streamable HTTP server exposing those read-only tools; LangGraph discovers them through `langchain-mcp-adapters`.
- A CrewAI specialist team for destination research, mobility research and itinerary drafting. The Copilot exposes this through **Plan with AI team**.

Follow `AGENTIC_AI_IMPLEMENTATION.md` for the four-terminal Windows startup procedure. The sidecar is optional and disabled by default; the existing direct LLM path remains available as a fallback. The agent tools are read-only and do not book or purchase anything. Real-time schedules remain dependent on connected operator feeds.
