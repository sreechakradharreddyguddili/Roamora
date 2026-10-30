# Roamora — Stays, Explore and Transport reliability update

## Stays
- Selecting a country no longer leaves a warning as the only response. A country-level selection is correctly cleared when the user picks a city, preventing the country coordinates from accidentally being reused for the city search. India shows the existing state/city browser; other country-level results show popular city suggestions where available plus a city/town field that works for other countries too.
- Search runs the chosen radius and 25 km fallback concurrently, so an empty small-radius result does not require a second serial wait.
- Hotel results are ordered to prefer named listings, published map star classifications, then distance. Star classifications are shown only when the map source supplies them.
- Hotel, hostel, guest-house and resort filters remain separate. Data is sourced from OpenStreetMap; this does not provide guaranteed booking availability, room prices or review rankings.
- If the map provider has no listing for the chosen place, the UI offers a Google Maps search instead of leaving the user stuck.

## Explore
- Nearby discovery searches a broad radius and, when a category has sparse map tagging, requests the general nearby set concurrently.
- Popular destination cards start the location search directly.
- Empty map results provide a direct Google Maps fallback rather than a dead end.
- Backend place search now queries two public Overpass mirrors concurrently with a shared short deadline and broader tags for food, accommodation and attractions. Empty results are cached briefly to avoid repeated slow provider calls.

## Transport
- Added a global transport-hub directory search by city/country using Roamora's indexed global transport-place endpoint.
- Added a service-date field to scheduled departures and passes that date to the existing GTFS departure endpoint.
- Added a city/country/operator filter over the configured GTFS feeds.
- Public read-only global transport-hub search is permitted without login; administrative endpoints remain protected.
- Live positions, arrival estimates and delays are displayed only when a compatible operator feed provides them.

## Important data coverage limits
A universal live tracker for every train, bus, taxi, private car and town worldwide cannot be created from frontend changes alone. Each operator needs a compatible schedule feed and, for live tracking, a real-time feed/API and appropriate access. The app deliberately distinguishes mapped hubs and published schedules from realtime data; it must not fabricate positions or arrival times.

## Validation performed
- TypeScript compiler parser/transpiler syntax checks passed for modified JSX/JS files.
- CSS delimiter balance checks passed for modified stylesheets.
- ZIP integrity is checked when packaging.
- Full Maven compilation and browser testing were unavailable in the build environment because Maven could not download its distribution. Run the project locally and verify provider connectivity before production use.

## Follow-up reliability update (29 Oct 2026)
- Increased OpenStreetMap Overpass connection/read tolerance and the shared response window. The earlier 1.5-second read timeout and 1.75-second total deadline could cause popular-city queries to return empty before the map provider had time to answer.
- Nearby place and transport-map queries now wait for the first completed usable response rather than blocking on the first endpoint in the list.
- Added a public `/api/v1/places/transport-nearby` endpoint to discover named railway/metro stations, bus stops, ferry terminals, and airports from public OpenStreetMap data around a geocoded city.
- Transport hub search now combines indexed transport directory results with nearby mapped hubs. These are locations, not timetables; schedule/departure data still requires an imported operator feed.
- Explore now sorts real rating fields first when a source provides them and explicitly labels missing review ratings. OpenStreetMap `stars` tags are not treated as public customer-review ratings.
- Stays likewise prioritizes genuine rating fields when supplied, then named listings, mapped accommodation class, and distance. Map-only sources do not provide review ratings or live prices by default.


## Country, city/town and transport mode browsing follow-up
- Explore now opens a country directory when a country is selected, with suggested cities/towns and a custom city/town search. The existing All places, Food, Cafés, Stays and Things to see filters remain available before results are searched.
- Stays quick destinations include additional smaller towns and tourist destinations; the custom location field accepts other city/town names. Results remain limited to mapped accommodation listings, not guaranteed room availability.
- Transport includes dedicated mode cards for rail, bus/coach, metro, airports, ferries and taxis/car hire, map links, and Today/Tomorrow schedule shortcuts. Operator schedules and second-by-second vehicle tracking remain dependent on real feeds; only configured real-time feeds are polled every 15 seconds.
