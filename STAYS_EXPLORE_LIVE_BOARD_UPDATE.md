# Roamora Stays, Explore and Today Live Board Update

## Stays
- Added image URL support from map tags with a category-specific representative image fallback.
- Added explicit listed price/fee display when public map data supplies it; otherwise the card says price is not listed.
- Added official website / website-search, map, and price-search links.
- Made the 1–5 minimum rating/class filter apply to hotels, resorts, hostels and guest houses when the map source has a rating/class value.
- Broadened resort map search to include `tourism=resort`, `leisure=resort`, and common resort-tagged listings.

## Explore
- Place cards now show place images where mapped, representative images otherwise, listed rating/class information when present, listed price/fee when present, and map, website and price-search links.
- Customer ratings and prices are not fabricated if public map data does not supply them. A licensed review/booking API is needed for comprehensive ratings and real prices.

## Transport
- Removed tomorrow/future-date controls; the stop board is for today's services.
- Live-enabled feeds refresh the selected stop board and vehicle/trip information every 15 seconds while the page is open. Schedule-only feeds remain labelled as scheduled rather than live.
- Live services are only available for operators whose actual timetable/realtime feeds are configured.

## Validation
- Reviewed Java record construction sites and updated the only `PlaceResponse` constructor for the new optional image and price fields.
- Full Maven/frontend build and live provider tests require the user's local environment and provider access.
