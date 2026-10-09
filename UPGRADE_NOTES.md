# Roamora Upgrade Notes — 2026-09-22

## UX / UI

- Added reusable anti-gravity motion to routed frontend pages.
- Motion uses requestAnimationFrame, off-screen skipping and reduced-motion support to avoid the hanging/jittery feeling common with heavy animation.
- Pointer position, scroll position and hover state create different subtle movement per card/visual.
- Kept the existing Roamora visual language as the base rather than replacing the product with a generic template.
- Removed the incorrect inline positioning that could pull the Risk experience card out of alignment on the home page.

## Roamora Intelligence

- Copilot is now global and available on every page.
- It explains the product when no journey exists.
- It understands the latest Planner analysis through a browser event bridge.
- It answers journey questions about weather, risk, duration, alternatives and preparation.
- It provides direct navigation actions to Planner, Explore and Stays.
- It supports Escape-to-close and focuses its input when opened.

## Authentication

- Wrong password and unknown email now resolve to the same clean `Invalid email or password` response.
- Duplicate signup email now uses HTTP 409 and a clear sign-in hint.
- Frontend API handling reads JSON error bodies rather than replacing them with generic fetch errors.
- True network failures receive a separate server-connectivity message.
- Authentication error UI was upgraded from a neutral gray box to a clear, restrained alert state.

## Account

- Replaced the broken duplicate Planner implementation in ProfilePage with a real account/settings screen.
- Profile data comes from the authenticated backend.
- Local travel preferences are persisted and immediately reflected.
- Added Profile to the authenticated header navigation.

## Reliability

- Added route scroll restoration after navigation.
- Added an application-level recovery boundary for unexpected UI errors.
- Dashboard and History now surface API failures instead of silently looking empty.
- Planner prevents obviously past departure times for today's date.
- API client consistently reports connectivity failures.

## Production note

The copilot in this build is a deterministic, context-aware assistant and does not claim to be a remote LLM. It is intentionally useful without requiring an AI API key. A real model-backed intelligence provider can be connected later behind the backend without exposing provider credentials to the browser.

## Optional real AI

The global copilot is now AI-ready without making an external provider mandatory. When both frontend and backend AI flags are enabled and a server-side provider key is configured, authenticated users can receive model-backed responses through `/api/v1/intelligence/chat`. If the provider is disabled or unavailable, the copilot automatically falls back to its local journey-aware response engine.

## Stays / Explore reliability pass — 2026-09-22

- Reworked nearby-stay retrieval to use a correctly encoded form POST and multiple public Overpass fallback endpoints, reducing failures caused by a single unavailable map-data instance.
- Added a typed-destination fallback: Stays and Explore can resolve a destination through the backend even when autocomplete suggestions are unavailable.
- Added mapped hotel star-class data from the OSM `stars` tag and a clean `Any / 1★ / 2★ / 3★ / 4★ / 5★` minimum-class selector.
- Star-class UI is explicitly described as mapped classification, not consumer review rating.
- Cleaned Explore feature-card link styling so cards remain clickable without browser-style underlines or link-like text treatment.
- Added visible nearby-place error handling in Explore instead of silently showing an empty state.
- Removed the partial frontend `node_modules` directory and the unused root `package-lock.json` from the release tree; dependencies are installed from `frontend/package-lock.json` with `npm install`/`npm ci`.
