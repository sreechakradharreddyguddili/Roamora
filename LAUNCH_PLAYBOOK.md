# Roamora Public Launch Playbook

Roamora is now positioned as a journey-intelligence product rather than a generic route finder.
The public experience should consistently communicate one idea:

> **Understand the journey before you begin.**

## Product experience shipped in this upgrade

- Motion is scroll-aware, pointer-aware and deliberately subtle.
- `prefers-reduced-motion` is respected.
- Global Roamora Intelligence copilot is available from every page.
- Copilot can explain Roamora, guide users to Planner / Explore / Stays, and answer questions about the latest analyzed journey.
- Planner broadcasts its current journey context to the copilot.
- Authentication errors now surface the server's real message instead of generic fetch errors.
- Unknown login email and wrong password use the same authentication message.
- Duplicate signup email returns a conflict response with a useful message.
- Profile is a real account page instead of a duplicate planner.
- Global route navigation scrolls to the top.
- A UI error boundary prevents one unexpected component error from leaving the entire application blank.
- Route search prevents obviously past departure times and keeps location suggestions from being reused after the input changes.

## Before production

1. Run a clean frontend install and production build.
2. Run a clean backend Maven build and application startup.
3. Configure PostgreSQL and Redis with production credentials.
4. Set a long random `APP_JWT_SECRET`.
5. Set `CORS_ALLOWED_ORIGINS` to the exact production frontend origin(s).
6. Set `VITE_API_URL` to the HTTPS backend API origin.
7. Configure a production routing service or self-hosted routing capacity before high traffic.
8. Configure live flight / transit providers only when valid credentials and coverage are available.
9. Keep unsupported transport modes explicitly unavailable; never fabricate schedules.
10. Add HTTPS, domain, privacy policy, terms, contact/support information and a clear data-retention policy.
11. Test authentication, route planning, route save/unsave, history, profile, Explore, Stays, mobile navigation and OAuth on a real production-like environment.
12. Add monitoring for API errors, latency, provider failures, database health and cache health.

## Recommended launch sequence

### Phase 1 — private beta

Invite a small group of travelers and test:

- Can they understand the product in five seconds?
- Can they plan a route without explanation?
- Do weather and risk explanations make sense?
- Do they trust the recommendation because the trade-offs are visible?
- Does the copilot answer useful questions without pretending to know unavailable data?

### Phase 2 — public launch

Launch around a specific use case first rather than advertising every capability at once. Good product stories include:

- weather-aware road trips
- safer-feeling route comparison with transparent context
- trip planning before departure
- discovering useful stops around a journey

### Phase 3 — growth

Add measurable product loops:

- shareable journey summaries
- saved journeys
- return-user dashboard
- personalized route preferences
- weather-triggered journey updates
- richer transport coverage
- real AI reasoning backed by live Roamora data

## Marketing foundation

### One-line positioning

**Roamora helps you understand a journey before you take it — connecting routes, weather and travel context in one place.**

### Short description

**Plan with context. Compare routes, understand conditions and make a more informed travel decision before you move.**

### Content ideas

- “A route is more than a line on a map.”
- “What if your route planner explained the journey?”
- “Fastest isn't always the route you want.”
- “Know the weather before the road tells you.”
- “Meet Roamora Intelligence.”

## Important product rule

Roamora should explain available evidence and uncertainty. It should never invent a flight, transit schedule, weather observation, route or safety guarantee when the underlying provider is unavailable.

## Optional model-backed Roamora Intelligence

The copilot works without an external model using local, context-aware responses. For authenticated users, a real model can be enabled through the backend without exposing the provider API key to the browser:

- Backend: `ROAMORA_AI_ENABLED=true`
- Backend: `ROAMORA_AI_API_KEY=<server-side secret>`
- Backend: `ROAMORA_AI_BASE_URL=<OpenAI-compatible chat endpoint>`
- Backend: `ROAMORA_AI_MODEL=<provider model>`
- Frontend: `VITE_ROAMORA_AI_ENABLED=true`

The model receives the user's question plus the journey context supplied by Roamora. The system instruction explicitly prohibits fabricating live route, weather, schedule, price or safety information.
