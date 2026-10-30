# Roamora — Journey Intelligence

Roamora is a global, weather-aware journey planning platform designed to help people understand a trip before they start it.

## What is included

- Worldwide location search through Open-Meteo geocoding
- Route planning with map geometry and alternative routes
- Weather-aware route analysis
- Route risk context and recommendations
- Public-transport / GTFS integration points
- Flight, marine and mobility integration points
- Nearby hotels, attractions, restaurants and essential places through OpenStreetMap/Overpass
- Accounts, saved journeys and route history
- Redis caching and PostgreSQL persistence
- JWT authentication and optional Google OAuth2
- Swagger/OpenAPI documentation
- Roamora Intelligence Copilot with route-aware explanations and quick questions
- Installable PWA shell and crawler-friendly metadata

## Product direction

The core Roamora idea is **Journey Intelligence**, not simply navigation. A route is treated as a combination of movement, weather, risk, time and context.


## Local development

### Backend

Requirements:
- Java 23
- PostgreSQL
- Redis

Spring Boot does not load `.env` files automatically. Set the required environment variables in the same terminal used to start the backend; see `AI_SETUP.md` for the Windows PowerShell example and Copilot configuration.

Run on Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Compile:

```powershell
cd backend
.\mvnw.cmd clean compile
```

### Frontend

Requirements:
- Node.js 22+

```powershell
cd frontend
npm install
npm run dev
```

Production build:

```powershell
npm run build
```

## Production checklist

Before launch:

1. Set a strong `APP_JWT_SECRET`.
2. Configure a managed PostgreSQL database.
3. Configure Redis.
4. Set `CORS_ALLOWED_ORIGINS` to the exact production frontend origin(s).
5. Configure the production frontend API base URL.
6. Configure OAuth credentials only if Google login is enabled.
7. Add production routing/transit providers where live schedules are required.
8. Review external provider terms, quotas and attribution requirements.
9. Configure HTTPS, domain, monitoring, backups and database migrations.
10. Do not commit `.env` files or API secrets.

## Important provider note

Roamora is provider-agnostic at the application level, but external routing, weather, geocoding, mapping, transit and flight services have different coverage, quotas and production terms. A global launch should configure production-grade providers and monitor their limits rather than relying on a single public endpoint for all traffic.

## Current frontend upgrade

The photo-led interface uses calm fixed layouts, responsive destination cards, real location search, map links based on returned coordinates, and representative hotel/food photos clearly labelled as illustrative. Broken remote photos fall back to a local placeholder. Decorative orbital animations have been removed. See `AI_SETUP.md` for Copilot setup and current capability boundaries.

See `LAUNCH_PLAYBOOK.md` for the production launch, QA and marketing checklist.

## Global product direction

Roamora is designed as a journey-intelligence layer rather than a generic booking clone. The current application includes a journey workspace, provider-agnostic configuration, grounded AI behavior, route/weather/risk context, stays and places, and profile preferences.

For production launch, enable licensed providers for the capabilities you actually promise: production maps/places, hotel inventory, flights, and regional transit. See `PROVIDER_ROADMAP.md`.

### Run locally

```bash
cd frontend
npm install
npm run dev
```

Backend environment variables are documented in `backend/.env.example`. Never put hotel, flight or LLM secret keys in the Vite frontend environment.

## Journey Resilience (new)

Open `/journey-resilience` to create a scenario-based travel preparation checklist for weather changes, route disruption, unavailable services, connectivity loss, or delays. The checklist can be downloaded as a text file and adapts to the travel mode and user-selected needs. This initial implementation is an offline planning aid, not a live disruption feed or a safety guarantee.

## Production security profile

For production, activate Spring's `prod` profile and provide explicit `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `APP_JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and `OAUTH2_AUTHORIZED_REDIRECT_URI` values. The production profile rejects missing JWT secrets and known placeholder/weak JWT secrets. Do not deploy using the local defaults in `application.properties`. See `RELEASE_REMEDIATION_PLAN.md` for the remaining release gates.
