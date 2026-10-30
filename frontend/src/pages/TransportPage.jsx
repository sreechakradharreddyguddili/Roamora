import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Activity, AlertTriangle, ArrowRight, Clock3, MapPin, RefreshCw, Search, TrainFront, Wifi, WifiOff } from "lucide-react";
import { Link } from "react-router-dom";
import { getGtfsDepartures, getLiveTripUpdates, getLiveVehicles, getTransportAlerts, getTransportFeeds, searchGtfsStops, searchTransportHubs, searchTransportMapPlaces } from "../api/transportApi";
import { resolveLocation } from "../api/routeApi";
import "./TransportPage.css";

function formatDateTime(value) {
  if (!value) return "Not supplied";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

function localDateValue(date = new Date()) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function delayLabel(seconds) {
  if (seconds === null || seconds === undefined || !Number.isFinite(Number(seconds))) return "Delay not supplied";
  const minutes = Math.round(Number(seconds) / 60);
  if (minutes <= 0) return "On time / early";
  return `${minutes} min delayed`;
}

function mapVehicleUrl(vehicle) {
  if (Number.isFinite(Number(vehicle.latitude)) && Number.isFinite(Number(vehicle.longitude))) {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${vehicle.latitude},${vehicle.longitude}`)}`;
  }
  return null;
}

export default function TransportPage() {
  const [feeds, setFeeds] = useState([]);
  const [selectedFeedId, setSelectedFeedId] = useState("");
  const [vehicles, setVehicles] = useState([]);
  const [updates, setUpdates] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loadingFeeds, setLoadingFeeds] = useState(true);
  const [loadingLive, setLoadingLive] = useState(false);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);
  const [stopQuery, setStopQuery] = useState("");
  const [feedSearch, setFeedSearch] = useState("");
  const [hubSearch, setHubSearch] = useState("");
  const [hubMode, setHubMode] = useState("ALL");
  const [transportHubs, setTransportHubs] = useState([]);
  const [loadingHubs, setLoadingHubs] = useState(false);
  const [hubError, setHubError] = useState("");
  const [stops, setStops] = useState([]);
  const [selectedStop, setSelectedStop] = useState(null);
  const [departures, setDepartures] = useState([]);
  const [loadingStops, setLoadingStops] = useState(false);
  const [loadingDepartures, setLoadingDepartures] = useState(false);
  const liveRequestInFlight = useRef(false);
  const departureRequestId = useRef(0);

  const selectedFeed = useMemo(() => feeds.find((feed) => feed.feedId === selectedFeedId) || null, [feeds, selectedFeedId]);
  const visibleFeeds = useMemo(() => {
    const query = feedSearch.trim().toLowerCase();
    if (!query) return feeds;
    return feeds.filter((feed) => [feed.providerName, feed.city, feed.region, feed.country, feed.feedId].filter(Boolean).join(" ").toLowerCase().includes(query));
  }, [feeds, feedSearch]);

  const loadFeeds = useCallback(async () => {
    setLoadingFeeds(true);
    setError("");
    try {
      const result = await getTransportFeeds();
      setFeeds(result);
      setSelectedFeedId((current) => current && result.some((feed) => feed.feedId === current) ? current : (result.find((feed) => feed.realtimeEnabled)?.feedId || result[0]?.feedId || ""));
    } catch (requestError) {
      setError(requestError?.message || "Transport provider coverage is temporarily unavailable.");
    } finally {
      setLoadingFeeds(false);
    }
  }, []);

  const realtimeReady = Boolean(selectedFeed?.realtimeEnabled);

  const loadLiveData = useCallback(async () => {
    if (!selectedFeedId || !realtimeReady || liveRequestInFlight.current) {
      if (!selectedFeedId || !realtimeReady) {
        setVehicles([]); setUpdates([]); setAlerts([]);
      }
      return;
    }
    liveRequestInFlight.current = true;
    setLoadingLive(true);
    setError("");
    try {
      const [nextVehicles, nextUpdates, nextAlerts] = await Promise.all([
        getLiveVehicles(selectedFeedId), getLiveTripUpdates(selectedFeedId), getTransportAlerts(selectedFeedId),
      ]);
      setVehicles(nextVehicles);
      setUpdates(nextUpdates);
      setAlerts(nextAlerts);
      setLastUpdated(new Date());
    } catch (requestError) {
      setError(requestError?.message || "Live updates could not be retrieved for this provider.");
    } finally {
      liveRequestInFlight.current = false;
      setLoadingLive(false);
    }
  }, [selectedFeedId, realtimeReady]);

  useEffect(() => { loadFeeds(); }, [loadFeeds]);
  useEffect(() => {
    loadLiveData();
    if (!selectedFeedId || !realtimeReady) return undefined;
    const timer = window.setInterval(loadLiveData, 15000);
    return () => window.clearInterval(timer);
  }, [loadLiveData, selectedFeedId, realtimeReady]);

  useEffect(() => {
    if (!selectedFeedId || !realtimeReady || !selectedStop) return undefined;
    let active = true;
    const refreshBoard = async () => {
      try {
        const result = await getGtfsDepartures(selectedFeedId, selectedStop.stopId, 12, localDateValue());
        if (active) { setDepartures(Array.isArray(result?.departures) ? result.departures : []); setLastUpdated(new Date()); }
      } catch { /* Keep the last good board visible if a live refresh temporarily fails. */ }
    };
    const timer = window.setInterval(refreshBoard, 15000);
    return () => { active = false; window.clearInterval(timer); };
  }, [selectedFeedId, realtimeReady, selectedStop]);

  useEffect(() => {
    departureRequestId.current += 1;
    setSelectedStop(null); setStops([]); setDepartures([]); setStopQuery("");
  }, [selectedFeedId]);

  useEffect(() => {
    const query = stopQuery.trim();
    if (!selectedFeedId || query.length < 2) { setStops([]); setLoadingStops(false); return undefined; }
    let active = true;
    const timer = window.setTimeout(async () => {
      setLoadingStops(true);
      try {
        const result = await searchGtfsStops(selectedFeedId, query);
        if (active) setStops(result);
      } catch {
        if (active) setStops([]);
      } finally {
        if (active) setLoadingStops(false);
      }
    }, 180);
    return () => { active = false; window.clearTimeout(timer); };
  }, [selectedFeedId, stopQuery]);

  const findTransportHubs = async (event, requestedMode = hubMode, requestedSearch = hubSearch) => {
    event?.preventDefault();
    const searchValue = requestedSearch;
    const parts = searchValue.split(",").map((part) => part.trim()).filter(Boolean);
    if (!parts.length) {
      setHubError("Enter a city or transport station to search.");
      return;
    }
    setLoadingHubs(true);
    setHubError("");
    try {
      const country = parts.length > 1 ? parts[parts.length - 1] : undefined;
      const city = parts.length > 1 ? parts[0] : parts[0];
      let indexed = [];
      try { indexed = await searchTransportHubs({ city, country, limit: 50 }); } catch { /* OSM map fallback below can still work. */ }
      let mapped = [];
      try {
        const location = await resolveLocation(searchValue.trim());
        mapped = await searchTransportMapPlaces({ latitude: Number(location.latitude), longitude: Number(location.longitude), radiusKm: 25, limit: 60, type: requestedMode });
      } catch { /* Keep indexed provider results if geocoding or public map data is unavailable. */ }
      const byId = new Map();
      [...indexed, ...mapped].forEach((item) => {
        const key = item.id || `${item.name}-${item.latitude}-${item.longitude}`;
        if (!byId.has(key)) byId.set(key, item);
      });
      const modeNeedle = requestedMode === "ALL" ? "" : requestedMode;
      const result = [...byId.values()]
        .filter((item) => !modeNeedle || String(item.type || "").toUpperCase().includes(modeNeedle))
        .sort((a, b) => (Number(a.distanceKm) || 99999) - (Number(b.distanceKm) || 99999));
      setTransportHubs(result);
      const normalizedHubSearch = searchValue.trim().toLowerCase();
      const matchingFeed = feeds.find((feed) => {
        const feedLocation = [feed.city, feed.region, feed.country].filter(Boolean).join(" ").toLowerCase();
        const cityName = String(feed.city || "").trim().toLowerCase();
        return (feedLocation && feedLocation.includes(normalizedHubSearch)) || (cityName && normalizedHubSearch.includes(cityName));
      });
      if (matchingFeed) { setFeedSearch([matchingFeed.city, matchingFeed.country].filter(Boolean).join(" ")); setSelectedFeedId(matchingFeed.feedId); }
      if (!result.length) setHubError("No mapped transport hubs were returned for this location and mode. Try another nearby town, choose All modes, or open the map search. Published schedules require a compatible operator feed.");
    } catch (requestError) {
      setTransportHubs([]);
      setHubError(requestError?.message || "Transport hub search is temporarily unavailable.");
    } finally {
      setLoadingHubs(false);
    }
  };

  const chooseStop = async (stop) => {
    const requestedDate = localDateValue();
    const requestId = ++departureRequestId.current;
    setSelectedStop(stop); setStopQuery(stop.stopName || stop.stopId); setStops([]); setDepartures([]); setLoadingDepartures(true);
    try {
      const result = await getGtfsDepartures(selectedFeedId, stop.stopId, 12, requestedDate);
      if (requestId === departureRequestId.current) setDepartures(Array.isArray(result?.departures) ? result.departures : []);
    } catch {
      if (requestId === departureRequestId.current) setDepartures([]);
    } finally { if (requestId === departureRequestId.current) setLoadingDepartures(false); }
  };

  return <main className="transport-page">
    <section className="transport-hero">
      <div className="transport-hero-copy">
        <span className="transport-eyebrow">ROAMORA / TRANSPORT STATUS</span>
        <h1>Know when it’s moving.<br/><em>Know what’s next.</em></h1>
        <p>See scheduled and live transport information when an operator publishes a supported feed. Vehicle positions, arrival estimates and delays are shown only when the source provides them.</p>
        <div className="transport-hero-points"><span><Clock3 size={16}/> Arrival estimates</span><span><MapPin size={16}/> Vehicle positions</span><span><Activity size={16}/> Service updates</span></div>
      </div>
      <div className="transport-hero-art"><TrainFront size={54}/><div><strong>Live data, not guesses</strong><span>Live-enabled feeds and selected station boards refresh every 15 seconds while this page is open.</span></div></div>
    </section>

    <section className="transport-content">
      <section className="transport-stop-finder transport-worldwide-hubs">
        <div className="transport-section-heading"><div><span>WORLDWIDE TRANSPORT DIRECTORY</span><h2>Find stations and terminals</h2></div></div>
        <p>Search railway stations, bus stops, metro stations, airports, ferry terminals and other transport hubs by city and country. Roamora checks its transport directory and then public map data for nearby mapped hubs.</p>
        <div className="transport-mode-directory" aria-label="Transport modes">
          {[
            { mode: "RAIL", title: "Trains & rail", emoji: "🚆", query: "train stations", detail: "Stations, platforms and rail services" },
            { mode: "BUS", title: "Buses & coaches", emoji: "🚌", query: "bus stations and bus stops", detail: "Bus terminals and mapped stops" },
            { mode: "METRO", title: "Metro & subway", emoji: "🚇", query: "metro stations", detail: "Urban rail and subway stations" },
            { mode: "AIRPORT", title: "Airports", emoji: "✈️", query: "airports", detail: "Airports and terminals" },
            { mode: "FERRY", title: "Ferries & ports", emoji: "⛴️", query: "ferry terminals and ports", detail: "Ferry terminals and harbours" },
            { mode: "ROAD", title: "Taxis & car hire", emoji: "🚕", query: "taxi stands and car rental", detail: "Road transport providers via map search" },
          ].map((item) => <article className="transport-mode-card" key={item.mode}>
            <span className="transport-mode-emoji" aria-hidden="true">{item.emoji}</span><strong>{item.title}</strong><small>{item.detail}</small>
            <button type="button" onClick={() => { if (item.mode === "ROAD") { window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${item.query} in ${hubSearch || "your destination"}`)}`, "_blank", "noopener,noreferrer"); return; } setHubMode(item.mode); if (hubSearch.trim()) findTransportHubs(undefined, item.mode, hubSearch); }} disabled={item.mode !== "ROAD" && !hubSearch.trim()}>{item.mode === "ROAD" ? "Search map" : "Find near destination"}<ArrowRight size={13}/></button>
            <a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${item.query} in ${hubSearch || "your destination"}`)}`} target="_blank" rel="noreferrer">Open map search</a>
          </article>)}
        </div>
        <p className="transport-mode-hint">Enter a city or town below first, then choose a transport box. Map locations can be found worldwide; timetable and live movement details appear only for connected operators.</p>
        <form className="transport-hub-search" onSubmit={findTransportHubs}>
          <Search size={17}/>
          <input value={hubSearch} onChange={(event) => setHubSearch(event.target.value)} placeholder="Try Rome, Italy or London, United Kingdom" aria-label="City and country for transport hub search" />
          <select value={hubMode} onChange={(event) => setHubMode(event.target.value)} aria-label="Transport mode">
            <option value="ALL">All modes</option><option value="RAIL">Trains / rail</option><option value="METRO">Metro / subway</option><option value="BUS">Buses</option><option value="AIRPORT">Airports</option><option value="FERRY">Ferries</option>
          </select>
          <button type="submit" disabled={loadingHubs || !hubSearch.trim()}>{loadingHubs ? "Searching…" : "Find hubs"}</button>
        </form>
        {hubError && <div className="transport-error" role="status">{hubError}{transportHubs.length === 0 && hubSearch.trim() && <p><a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`train stations bus stops in ${hubSearch}`)}`} target="_blank" rel="noreferrer">Search transport hubs on Google Maps <ArrowRight size={14}/></a></p>}</div>}
        {transportHubs.length > 0 && <><div className="transport-hub-count">{transportHubs.length} mapped transport hub{transportHubs.length === 1 ? "" : "s"}</div><div className="transport-hub-grid">{transportHubs.map((hub) => <article key={hub.id || hub.name} className="transport-hub-card"><span>{String(hub.type || "TRANSPORT").replaceAll("_", " ")}</span><h3>{hub.name}</h3><p>{[hub.city, hub.region, hub.country].filter(Boolean).join(", ") || "Location details not supplied"}</p>{(hub.operatorName || hub.operator) && <small>Operator: {hub.operatorName || hub.operator}</small>}{(hub.code || hub.ref) && <small>Code / reference: {hub.code || hub.ref}</small>}{Array.isArray(hub.supportedModes) && hub.supportedModes.length > 0 && <small>Modes: {hub.supportedModes.map((mode) => String(mode).replaceAll("_", " ").toLowerCase()).join(", ")}</small>}<small>Source: {hub.source || "Connected transport directory"}{hub.distanceKm != null ? ` · ${Number(hub.distanceKm).toFixed(1)} km away` : ""}</small><a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(Number.isFinite(Number(hub.latitude)) && Number.isFinite(Number(hub.longitude)) ? `${hub.latitude},${hub.longitude}` : `${hub.name} ${hub.city || ""} ${hub.country || ""}`)}`} target="_blank" rel="noreferrer">View on map <ArrowRight size={14}/></a></article>)}</div><p className="transport-hub-note">Stations and stops are mapped from public geographic data. If a compatible timetable feed is connected for this city, use the departures section below for upcoming services. Map listings alone do not contain live departure boards.</p></>}
      </section>
      <div className="transport-section-heading"><div><span>01 / LIVE FEED COVERAGE</span><h2>Choose a transport operator</h2></div><button type="button" onClick={() => { loadFeeds(); if (selectedFeedId) loadLiveData(); }} disabled={loadingFeeds || loadingLive}><RefreshCw size={15}/> Refresh</button></div>
      {loadingFeeds ? <div className="transport-empty">Loading available transport feeds…</div> : feeds.length ? <div className="transport-feed-panel">
        <label htmlFor="transport-feed-search">Find a country, city, town or operator</label>
        <input id="transport-feed-search" className="transport-feed-search" value={feedSearch} onChange={(event) => {
          const value = event.target.value;
          const query = value.trim().toLowerCase();
          const matching = feeds.filter((feed) => !query || [feed.providerName, feed.city, feed.region, feed.country, feed.feedId].filter(Boolean).join(" ").toLowerCase().includes(query));
          setFeedSearch(value);
          setSelectedFeedId((current) => matching.some((feed) => feed.feedId === current) ? current : (matching[0]?.feedId || ""));
        }} placeholder="Try London, United Kingdom, or an operator…" />
        <label htmlFor="transport-feed">Available operator / city feeds ({visibleFeeds.length})</label>
        {visibleFeeds.length ? <select id="transport-feed" value={visibleFeeds.some((feed) => feed.feedId === selectedFeedId) ? selectedFeedId : ""} onChange={(event) => setSelectedFeedId(event.target.value)}>
          <option value="">Choose a matching feed</option>
          {visibleFeeds.map((feed) => <option key={feed.feedId} value={feed.feedId}>{[feed.providerName, feed.city, feed.region, feed.country].filter(Boolean).join(" · ")}</option>)}
        </select> : <div className="transport-empty compact"><strong>No configured feed matches that location.</strong><p>Roamora can search any location, but transport schedules and live positions are available only where a compatible operator feed has been connected. This avoids displaying invented services.</p></div>}
        {selectedFeed && <div className="transport-feed-meta"><span>{selectedFeed.providerName || "Transport operator"}</span><span>{[selectedFeed.city, selectedFeed.region, selectedFeed.country].filter(Boolean).join(", ")}</span><span className={realtimeReady ? "transport-status live" : "transport-status"}>{realtimeReady ? <><Wifi size={13}/> Real-time feed enabled</> : <><WifiOff size={13}/> Schedule-only / live feed not configured</>}</span></div>}
      </div> : <div className="transport-empty"><strong>No operator feeds are configured yet.</strong><p>Roamora needs official or public transport feeds for each city/operator. Add a provider feed to enable schedules and, when offered, real-time vehicle tracking.</p></div>}

      {selectedFeed && <section className="transport-stop-finder">
        <div className="transport-section-heading"><div><span>STOP LOOKUP</span><h2>Departures from a stop</h2></div></div>
        <p>Choose a station or stop to see today’s departures and arrivals in a station-board layout. Live estimates refresh every 15 seconds only when the operator publishes real-time data.</p>
        <div className="transport-today-live"><span className="transport-live-dot"/> TODAY’S BOARD <strong>{new Date().toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })}</strong><small>Live-enabled operator boards refresh every 15 seconds. Schedule-only feeds show published timetable times.</small></div>
        <label htmlFor="transport-stop-search">Stop or station name</label>
        <div className="transport-stop-search"><Search size={17}/><input id="transport-stop-search" value={stopQuery} onChange={(event) => { setStopQuery(event.target.value); setSelectedStop(null); setDepartures([]); }} placeholder="Type a station or stop name…" autoComplete="off" /></div>
        {loadingStops && <div className="transport-empty compact">Searching stops…</div>}
        {stops.length > 0 && <div className="transport-stop-results">{stops.map((stop) => <button type="button" key={stop.stopId} onClick={() => chooseStop(stop)}><MapPin size={15}/><span><strong>{stop.stopName}</strong><small>{stop.stopCode ? `Stop code ${stop.stopCode} · ` : ""}{stop.stopId}</small></span><ArrowRight size={14}/></button>)}</div>}
        {selectedStop && <div className="transport-departures"><h3>{selectedStop.stopName} · Today’s services</h3>{loadingDepartures ? <p>Loading departures…</p> : departures.length ? departures.map((departure, index) => <article key={departure.tripId || `${departure.routeId}-${index}`}><span className="transport-departure-number">{String(index + 1).padStart(2, "0")}</span><div><strong>{departure.routeShortName || departure.routeId || "Service"}</strong><span>{departure.destination || departure.routeLongName || departure.tripId || "Destination not supplied"}</span></div><div><small>Scheduled arrival</small><strong>{departure.scheduledArrival || "Not supplied"}</strong><small>Scheduled departure</small><strong>{departure.scheduledDeparture || "Not supplied"}</strong></div><div><small>Estimated arrival</small><strong>{departure.estimatedArrival || "No live estimate"}</strong><small>Estimated departure</small><strong>{departure.estimatedDeparture || "No live estimate"}</strong></div><span className={departure.isDelayed ? "transport-delay delayed" : "transport-delay"}>{departure.status || (departure.delaySeconds == null ? (departure.realtimeAvailable ? "Live data" : "Scheduled") : delayLabel(departure.delaySeconds))}</span></article>) : <p>No departures were found for this stop on the selected date. Try another stop or confirm the feed’s schedule has been imported.</p>}</div>}
      </section>}
      {error && <p className="transport-error" role="status">{error}</p>}
      {selectedFeed && <>
        <div className="transport-section-heading transport-live-heading"><div><span>02 / LIVE VEHICLES</span><h2>Vehicles and movement</h2></div><small>{lastUpdated ? `Checked ${lastUpdated.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}` : "Waiting for provider data"}</small></div>
        {!realtimeReady ? <div className="transport-empty"><WifiOff size={22}/><strong>Live tracking isn’t available for this feed.</strong><p>The operator’s live vehicle-position and delay feed is not configured. Roamora will not invent a vehicle location or arrival time.</p></div> : loadingLive && !vehicles.length && !updates.length && !alerts.length ? <div className="transport-empty">Fetching the latest operator update…</div> : <>
          <div className="transport-stats"><div><span>Vehicles with positions</span><strong>{vehicles.length}</strong></div><div><span>Trip / stop updates</span><strong>{updates.length}</strong></div><div><span>Service alerts</span><strong>{alerts.length}</strong></div></div>
          {vehicles.length > 0 ? <div className="transport-live-grid">{vehicles.map((vehicle, index) => { const mapUrl = mapVehicleUrl(vehicle); return <article className="transport-live-card" key={vehicle.id || vehicle.vehicleId || `${vehicle.tripId}-${index}`}><div className="transport-card-top"><span><TrainFront size={17}/> Vehicle {vehicle.vehicleId || index + 1}</span><small>{vehicle.currentStatus || "Position reported"}</small></div><h3>{vehicle.routeId ? `Route ${vehicle.routeId}` : "Live vehicle"}</h3><p>Trip: {vehicle.tripId || "Not supplied"}</p><p>Speed: {vehicle.speed == null ? "Not supplied" : `${Math.round(vehicle.speed * 3.6)} km/h`}</p><p>Last source update: {formatDateTime(vehicle.fetchedAt)}</p>{mapUrl && <a href={mapUrl} target="_blank" rel="noreferrer">View current position <ArrowRight size={14}/></a>}</article>; })}</div> : <div className="transport-empty compact">No live vehicle positions have been published in the latest feed snapshot.</div>}
          <div className="transport-section-heading transport-live-heading"><div><span>03 / ARRIVALS & DELAYS</span><h2>Stop and trip estimates</h2></div></div>
          {updates.length ? <div className="transport-update-list">{updates.map((update, index) => <article key={update.id || `${update.tripId}-${update.stopId}-${index}`}><div><strong>{update.routeId ? `Route ${update.routeId}` : "Trip update"}</strong><span>{update.tripId || "Trip ID unavailable"} · Stop {update.stopId || "not supplied"}</span></div><div><small>Scheduled</small><strong>{formatDateTime(update.scheduledTime)}</strong></div><div><small>Estimated</small><strong>{formatDateTime(update.estimatedTime)}</strong></div><span className={Number(update.delaySeconds) > 0 ? "transport-delay delayed" : "transport-delay"}>{delayLabel(update.delaySeconds)}</span></article>)}</div> : <div className="transport-empty compact">No arrival or delay updates were included in the latest provider data.</div>}
          {alerts.length > 0 && <div className="transport-alert-list">{alerts.map((alert, index) => <article key={alert.id || index}><AlertTriangle size={17}/><div><strong>{alert.headerText || alert.title || alert.alertId || "Operator service alert"}</strong><p>{alert.descriptionText || alert.message || "The operator did not provide a description."}</p></div></article>)}</div>}
        </>}
      </>}
      <div className="transport-footnote">Coverage depends on each transport authority’s data license and feed. This is not a universal tracker for every private car, taxi, bus or train. Nearby restaurants and accommodation can be found in <Link to="/explore">Explore</Link> and <Link to="/stays">Stays</Link>.</div>
    </section>
  </main>;
}
