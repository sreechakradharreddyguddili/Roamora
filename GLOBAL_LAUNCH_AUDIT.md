ROAMORA / WEATHERWISE GLOBAL LAUNCH AUDIT
============================================

Audit basis:
- Uploaded project: Weatherwise Route Planner.zip
- Source inspected from backend/src and frontend/src
- Existing generated build artifacts and .git metadata were not used as source of truth.

CHANGES APPLIED IN THIS PATCH
-----------------------------

1. Route API cleanup
   - Added/kept getSavedRoutes().
   - Kept clearRouteHistory().
   - Removed stale getRouteHistory() because the backend has DELETE /routes/history
     but no GET /routes/history endpoint.

2. Weather data mapping
   - Open-Meteo current weather now requests temperature, wind, weather code,
     relative humidity, and precipitation.
   - RouteAnalysisResponse now exposes endpoint humidity, precipitation, and
     weather-code fields.
   - Frontend RouteAnalysis now maps those backend scalar fields into the
     weather cards. This fixes the screenshot where temperature/wind/humidity/
     precipitation showed dashes despite weather being available.

3. Route availability / history integrity
   - A provider-unavailable route (routeKey=UNAVAILABLE) is no longer persisted
     into authenticated user history.
   - Unavailable routes are never marked as "Recommended".
   - RouteAnalysisResponse now explicitly reports mapAvailable and selected GTFS
     journey metadata.
   - This prevents records such as the old Afghanistan -> Angola unavailable
     flight from being treated as a completed journey.
   - Existing bad historical records are not deleted automatically; clear them
     from History after deploying the patch.

4. Dashboard correctness
   - Dashboard statistics and recent journeys now ignore incomplete/unavailable
     route records that have neither distance nor duration.

5. History ordering
   - Route history and saved-route repository queries now sort by createdAt
     descending, so newest journeys appear first.

6. Departure schedule persistence
   - Added departure_date and departure_time to routes.
   - Added Flyway migration V12.
   - Route history responses now expose the requested departure date/time.
   - This preserves scheduled-journey context after saving.

7. Authentication error handling
   - Invalid credentials now return HTTP 401 instead of being treated as a
     generic 400 runtime error.
   - Generic runtime errors no longer expose Java exception class names to clients.
   - Invalid JSON responses no longer expose internal parser details.
   - Server-side logging retains the exception details.

8. Security hardening
   - Route planning POST remains public.
   - Route history/saved/delete endpoints now require authentication.
   - GTFS ingestion/feed mutation/realtime refresh are ADMIN-only.
   - Global transport imports are ADMIN-only.
   - Actuator metrics are no longer publicly exposed.

9. Production configuration
   - Database URL/user/password are environment-configurable.
   - JWT secret/expiration are environment-configurable.
   - CORS origins are environment-configurable.
   - Aviationstack configuration is environment-configurable.
   - Added backend/.env.example and frontend/.env.example.
   - Google login UI is hidden by default until explicitly enabled with
     VITE_GOOGLE_AUTH_ENABLED=true.

10. Route-plan HTTP semantics
    - POST /api/v1/routes/plan now returns HTTP 200 rather than 201 because
      planning is not necessarily creation of a persisted resource (anonymous
      and unavailable plans have no saved route ID).

IMPORTANT REMAINING GLOBAL-LAUNCH REQUIREMENTS
----------------------------------------------

A. Transit coverage is NOT globally complete yet.
   The current YAML contains one static GTFS feed (Carousel Buses, UK).
   OSM import supplies infrastructure/place data, not complete scheduled
   journeys. GTFS realtime, Mobility Database, flights, marine and GBFS are
   disabled unless configured.

B. Live flight schedules require a real provider/API key.
   Aviationstack support exists but is disabled when AVIATIONSTACK_API_KEY is
   empty. No flight should be fabricated.

C. The current road router is the public OSRM service.
   For a high-volume global launch, use a production routing provider or
   self-hosted/managed routing infrastructure with explicit capacity/SLA and
   monitoring.

D. Google OAuth is still intentionally disabled by default.
   Configure the backend OAuth client and frontend VITE_GOOGLE_AUTH_ENABLED only
   when the provider credentials and redirect URI are ready.

E. Production secrets must be supplied through environment/secret management.
   Do not use the development JWT/database defaults in production.

F. Existing invalid route-history rows from earlier testing may remain in the
   database. Clear them once after deployment if they are not wanted.

VALIDATION
----------

- Plain JavaScript API files were syntax-checked successfully.
- Full Maven compilation could not be executed in this environment because the
  Maven wrapper could not download Maven dependencies (network/cache limitation).
- Full Vite build could not be executed because npm dependencies were not
  available locally and offline installation lacked cached packages.

NEXT LIVE TESTS AFTER COPYING THE PATCH
---------------------------------------

1. Start PostgreSQL + Redis.
2. Start Spring Boot; verify Flyway applies V12.
3. Start Vite with VITE_API_URL.
4. Login with a valid account.
5. Plan Nellore -> Chennai by CAR:
   - map
   - distance
   - duration
   - temperature
   - humidity
   - precipitation
   - wind
   - risk
6. Plan an unsupported/uncertain FLIGHT route:
   - it must say live flight information is unavailable
   - it must NOT create a history row
   - it must NOT show a fake road map
7. Save a valid route, reload History, verify it is first/newest.
8. Clear history and verify only unsaved history is removed.
9. Test logout/login and expired-token refresh.

POST-AUDIT UX HARDENING / 2026-09-22
------------------------------------

    - Added a reusable frontend motion layer around every routed page.
    - Cards, panels, visual blocks and journey surfaces receive small scroll/pointer drift.
    - Motion is frame-scheduled and skips off-screen elements.
    - Reduced-motion users receive no active drift.

12. Global Roamora Intelligence
    - Copilot is now mounted at application level rather than only on Planner.
    - It provides general product guidance and journey-aware answers.
    - Planner broadcasts the latest route context to the copilot.
    - Navigation actions expose Planner, Explore and Stays directly from the assistant.

13. Authentication UX
    - Network failures now receive a specific server-connectivity message.
    - HTTP 401 authentication responses preserve the backend's `Invalid email or password` message.
    - Unknown login emails now return 401 rather than 400.
    - Duplicate registration email now returns 409 with a user-facing sign-in hint.

14. Profile correctness
    - Replaced the previous duplicate-planner implementation in ProfilePage with an actual profile/settings experience.
    - Profile loads account data through the authenticated API client.
    - Journey preference controls persist locally.

15. Navigation and resilience
    - Route changes reset scroll position.
    - Added an application error boundary with a recoverable reload state.
    - Added Profile to authenticated navigation.

16. Planner input quality
    - Prevents selecting a departure time that has already passed today.

17. Optional model-backed Roamora Intelligence
    - Added authenticated `/api/v1/intelligence/chat` endpoint.
    - Provider credentials remain server-side.
    - The frontend falls back to the local context-aware assistant when model support is disabled or unavailable.
    - AI is disabled by default and must be explicitly configured in both frontend and backend environments.
