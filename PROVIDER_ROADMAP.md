# Roamora Provider Roadmap

Roamora is deliberately provider-agnostic. The application should run without paid provider credentials in local/demo mode, while production adapters can be enabled independently.

## Recommended production order

1. **Maps + Places:** Google Maps Platform or Mapbox for production-grade search/routing/places. Keep OSM as a fallback/open-data source where licensing and usage policy allow.
2. **Hotels:** Booking.com Demand API or another licensed accommodation supplier for live availability, prices, photos and booking/redirect flows.
3. **Flights:** Amadeus or Duffel. Choose based on whether Roamora needs search-only or booking/ancillary workflows.
4. **Weather:** Open-Meteo initially; add a commercial provider only if forecast coverage, SLA, commercial terms or rate limits require it.
5. **Transit:** GTFS/GTFS-Realtime plus regional feeds. Do not claim worldwide live transit until provider coverage is verified region-by-region.
6. **AI:** OpenAI server-side with retrieval/RAG. Keep API keys out of the browser.

## Product rule

Roamora must never invent a route, hotel price, flight, schedule, live delay, review, weather value or safety guarantee. If a provider does not return a capability, show **Unavailable** and explain the source/last update when possible.

## Cost controls

- Cache geocoding, place details and repeated route requests.
- Use field masks with Google Places.
- Prefer one normalized provider response internally rather than leaking provider-specific schemas into React.
- Add quotas and daily spend alerts before public launch.
- Log provider latency, error rate, quota failures and stale-data age.
