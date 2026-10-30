# Roamora reliability and experience update

## Included improvements

- Redis cache key namespace moved to `roamora:v3` so values written with the previous serializer format are not read back as if they used the new format.
- In-memory TTL caches and request de-duplication for location suggestions and nearby-place searches; empty responses are briefly cached to avoid hammering a provider during a transient outage.
- Faster bounded HTTP timeouts for public geocoding and nearby-place requests.
- Latest-search-wins guards in Explore and Stays to stop older, slower searches overwriting the destination the user just selected.
- Copilot local guidance for travel planning, Roamora navigation, transport data, food, accommodation, budgets, packing, accessibility, visas, seasonality, bookings and safety. App navigation actions can open the relevant page and pass a destination when supported.
- Multi-turn history passed to the configured LLM provider; provider calls have a bounded timeout and fall back to local guidance.
- Login redirects preserve the original destination so users return to the feature they requested after authentication.
- Transport refreshes realtime-enabled feeds every 15 seconds in the UI without overlapping requests; the backend polls configured GTFS-RT provider feeds every 30 seconds by default and adds stop/station search plus scheduled departures and provider-supplied estimates.
- Read-only GTFS stop, departure and realtime endpoints are public; ingestion remains restricted.
- `START_ROAMORA.ps1` opens backend and frontend terminals for local development.

## Local startup

1. Start PostgreSQL and Redis.
2. From this folder, run `powershell -ExecutionPolicy Bypass -File .\START_ROAMORA.ps1`.
3. Open `http://localhost:5173`.
4. For an LLM-backed chatbot, follow `AI_SETUP.md` and configure a server-side provider key. The local travel-aware copilot still works without a key; never put provider secrets in frontend environment variables.

## Speed expectations

Cached searches and already-loaded screens should feel immediate. First-time searches depend on public geocoding/map providers and their network response times; a hard two-second guarantee is not possible for every external provider. Requests have bounded timeouts, duplicate calls are coalesced, and the UI should display loading/error states instead of waiting indefinitely.

## Validation status

- Frontend JS/JSX syntax checked across all files in `frontend/src`.
- CSS brace/parenthesis checks passed for changed stylesheets.
- ZIP integrity checks are run before delivery.
- Full npm build and Maven compile could not be run in this environment: npm dependency installation was unavailable/offline and the Maven wrapper could not download its distribution. Run `npm ci && npm run build` and `backend\mvnw.cmd -DskipTests compile` on the development machine to complete build verification.
