# Roamora release remediation and product roadmap

This plan is intended to help launch Roamora independently. No Anna integration is required.

## Changes included in this archive

- Added `/journey-resilience`, a user-input-driven journey contingency planner with route context, disruption scenarios, personalized preparation actions, a downloadable text checklist, and explicit non-live-data/safety disclaimers.
- Linked the new planner from Journey Intelligence.
- Added refresh-token rotation on refresh requests. The previous refresh token is revoked and a replacement is issued; the repository obtains a pessimistic write lock to reduce concurrent reuse.
- Added unit tests for refresh-token rotation, expiration, and revocation.
- Added a production Spring profile that requires database credentials, JWT secret, CORS origins, and OAuth redirect configuration to be explicitly supplied.
- Added production JWT secret validation so missing secrets fail startup and known placeholder/weak secrets are rejected when the `prod` profile is active.

## Important limitations

These changes do not constitute a complete production certification. The new resilience planner is a preparation checklist and scenario planner; it does not query live weather, road closures, incident feeds, emergency services, or provider status. It must not be described as live risk prediction.

The refresh-token change should be exercised against the actual PostgreSQL schema and frontend refresh flow before deployment. A secure HttpOnly-cookie migration is not included because it requires coordinated backend, CORS, CSRF, OAuth callback, and frontend session changes.

## Remaining P0 work before a public launch

1. Run `npm ci`, `npm run build`, and `npm run lint` from `frontend/`.
2. Run `./mvnw clean test` from `backend/` with a supported JDK and dependency access.
3. Test registration/login/refresh/logout against a clean PostgreSQL database and verify concurrent refresh-token replay fails.
4. Remove development-only defaults from production deployment variables and activate Spring profile `prod`.
5. Configure production database, Redis (if enabled), HTTPS, CORS, backups, secret management and monitoring.
6. Add a persistent/distributed rate limiter for login, registration, refresh, AI, geocoding and route provider calls. The environment example's budget/rate settings are not themselves enforcement.
7. Configure actual provider credentials and verify terms, quota, latency, source freshness, attribution and regional coverage.
8. Add browser end-to-end tests for signup → planner → route → workspace → stays → profile, plus provider outage tests.
9. Review privacy policy, terms, account deletion, consent, retention, accessibility and launch-jurisdiction requirements.

## Product roadmap

### First release: reliability and trust
- Evidence/freshness labels for route, weather, place, flight and transit data.
- Distinguish verified facts, estimates, stale results and unavailable capabilities.
- Route fallback and disruption checklist with explicit assumptions.
- Provider timeouts, circuit breakers, caching, quotas and observability.

### Differentiating features
- Journey resilience: scenario-based fallback preparation, later upgraded with verified live disruption feeds.
- Constraint-aware planning: walking limits, rest needs, accessibility requirements and family travel constraints.
- Offline journey pack: save itinerary and last-updated data for low-connectivity trips, with provider licensing respected.
- True journey cost: known costs and clearly labelled estimates, never fabricated prices.
- Privacy-first trip sharing: explicit consent, revocable access, arrival windows, and user-controlled check-ins.
- Outcome feedback: opt-in comparisons between estimated and actual trip outcomes to improve reliability.

## Release policy

Do not claim global live coverage unless the providers are configured and tested in each claimed region. Never present a generic preparedness checklist as a live alert, medical/emergency instruction, or safety guarantee.
