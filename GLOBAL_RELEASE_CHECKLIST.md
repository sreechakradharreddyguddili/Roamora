# Roamora Global Release Checklist

## Product
- [x] Route planner
- [x] Weather/risk context
- [x] Maps
- [x] Places
- [x] Stays discovery
- [x] Authentication/profile
- [x] History/saved routes
- [x] Journey-aware AI entry point
- [x] Journey Workspace for preparation, notes, sharing and discovery
- [x] Terms/privacy navigation

## Before public launch
- [ ] Configure a production map/places provider
- [ ] Configure licensed hotel inventory if prices/availability are advertised
- [ ] Configure a flight provider if flight results are advertised
- [ ] Add regional GTFS/GTFS-Realtime feeds where live transit is promised
- [ ] Configure OpenAI server-side and retrieval/vector storage for production RAG
- [ ] Add domain, HTTPS, secrets manager and database backups
- [ ] Add provider quotas, budget alerts and rate limiting
- [ ] Add product analytics with consent/privacy controls
- [ ] Run browser E2E tests for signup → planner → route → workspace → stays → profile
- [ ] Review legal text with counsel for launch jurisdictions

## Product principle
Never fabricate availability. Clearly label provider, freshness and unavailable capabilities.
