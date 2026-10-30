# Roamora location, place-search and transport update

## What changed
- Added a new **Transport** page with operator-feed selection, live vehicle positions, trip/stop updates, service alerts and a 30-second refresh while the page is open.
- Added an API client for the existing GTFS feed and real-time endpoints.
- Added state and city browsing for India on the Stays page. Choosing a city runs the nearby-stay search against that city's resolved coordinates instead of the country's centroid.
- Enriched location suggestions with region/district metadata and block country-level searches for local places, which prevents misleading nearby results for an entire country.
- Namespaced Redis cache keys (`roamora:v2`) so values written by an older serializer are not read by the current cache serializer.
- Bounded geocoding and Overpass HTTP timeouts, shortened Overpass query timeouts and stopped trying more public Overpass endpoints after a successful response (including a valid empty result).
- Added Transport to the header and a link from Explore.
- Preserved the How It Works and About image changes and the header underline CSS fix.

## Data coverage limits
Live vehicle locations and arrival estimates are only available where a transport operator provides supported GTFS/GTFS-Realtime data and the feed is configured in the backend. This cannot honestly track every private car, taxi, bus or train worldwide without those providers. If a selected feed does not publish live positions or trip updates, the UI reports that rather than inventing locations or timings. Nearby places and stays are based on OpenStreetMap data and can be incomplete in some areas.

## Run locally
1. Start PostgreSQL and Redis.
2. Start the backend from `backend` using the project's Maven wrapper and your existing environment settings.
3. Start the frontend from `frontend` with `npm run dev`.
4. Open **Transport** to inspect configured operator feeds; open **Stays** and search `India` to browse states and cities; open **Explore** and search a city such as Rome for mapped restaurants and attractions.

## Verification note
The source edits and archive structure were checked, but a full build could not be completed in this environment because Maven/npm dependencies could not be downloaded. Run the backend compile and frontend build locally before treating this as release-verified.
