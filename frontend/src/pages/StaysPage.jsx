import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  BedDouble,
  Compass,
  ExternalLink,
  Hotel,
  MapPin,
  Search,
  Sparkles,
  Star,
} from "lucide-react";
import { Link } from "react-router-dom";

import { resolveLocation, searchLocations } from "../api/routeApi";
import { getNearbyPlaces } from "../api/placeApi";

import "./StaysPage.css";

const stayFilters = [
  { label: "Hotels", value: "HOTEL", icon: Hotel },
  { label: "Hostels", value: "HOSTEL", icon: BedDouble },
  { label: "Guest houses", value: "GUEST_HOUSE", icon: BedDouble },
  { label: "Resorts", value: "RESORT", icon: Sparkles },
];

const starFilters = [0, 1, 2, 3, 4, 5];

function formatDistance(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "";
  }
  return `${Number(value).toFixed(1)} km away`;
}

function formatCategory(value) {
  return value?.replaceAll("_", " ") || "Stay";
}

function StarClass({ value }) {
  const rating = Number(value);

  if (!Number.isFinite(rating)) {
    return <span className="stay-star-unknown">Star class not listed</span>;
  }

  return (
    <span className="stay-stars" aria-label={`${rating} star mapped class`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          size={13}
          strokeWidth={1.8}
          fill={star <= Math.round(rating) ? "currentColor" : "none"}
        />
      ))}
      <small>{Number.isInteger(rating) ? rating : rating.toFixed(1)}★ mapped class</small>
    </span>
  );
}

function StaysPage() {
  const [query, setQuery] = useState("");
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [category, setCategory] = useState("HOTEL");
  const [radius, setRadius] = useState("8");
  const [starFilter, setStarFilter] = useState(0);
  const [places, setPlaces] = useState([]);
  const [loading, setLoading] = useState(false);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const value = query.trim();
    if (value.length < 2 || selectedLocation) {
      return undefined;
    }

    let active = true;
    const timer = window.setTimeout(async () => {
      try {
        setSuggestionLoading(true);
        const results = await searchLocations(value);
        if (active) setSuggestions(results.slice(0, 6));
      } catch {
        if (active) setSuggestions([]);
      } finally {
        if (active) setSuggestionLoading(false);
      }
    }, 280);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [query, selectedLocation]);

  const canSearch = Boolean(
    selectedLocation?.latitude !== undefined &&
      selectedLocation?.longitude !== undefined
  );

  async function runSearch(nextCategory = category) {
    let location = selectedLocation;

    if (!location || location.latitude === undefined || location.longitude === undefined) {
      if (query.trim().length < 2) {
        setError("Enter a destination first.");
        return;
      }

      try {
        setSuggestionLoading(true);
        location = await resolveLocation(query);
        setSelectedLocation(location);
        setQuery(
          location.country
            ? `${location.name}, ${location.country}`
            : location.name
        );
        setSuggestions([]);
      } catch (requestError) {
        setError(
          requestError?.message ||
            "We could not resolve that destination. Try a city or landmark name."
        );
        return;
      } finally {
        setSuggestionLoading(false);
      }
    }

    setLoading(true);
    setError("");

    try {
      const result = await getNearbyPlaces({
        latitude: location.latitude,
        longitude: location.longitude,
        category: nextCategory,
        radiusKm: Number(radius),
        limit: 50,
      });

      setPlaces(result);
      setSearched(true);

      if (!result.length) {
        setError(
          "No mapped stays were found in this area. Try a wider radius, another stay type, or another destination."
        );
      }
    } catch (requestError) {
      setPlaces([]);
      setSearched(true);
      setError(
        requestError?.message ||
          "Nearby stay information is temporarily unavailable."
      );
    } finally {
      setLoading(false);
    }
  }

  const filteredPlaces = useMemo(() => {
    if (category !== "HOTEL" || starFilter === 0) {
      return places.slice(0, 18);
    }

    return places
      .filter((place) => {
        const rating = Number(place.starRating);
        return Number.isFinite(rating) && rating >= starFilter;
      })
      .slice(0, 18);
  }, [category, places, starFilter]);

  const starFilterLabel = starFilter === 0 ? "Any class" : `${starFilter}★+`;

  return (
    <main className="stays-page">
      <section className="stays-hero">
        <div className="stays-hero-copy">
          <span className="stays-eyebrow">ROAMORA / STAYS</span>
          <h1>
            Stay close to
            <em> the journey.</em>
          </h1>
          <p>
            Discover mapped hotels, hostels, guest houses and resorts around
            your destination. Roamora uses location data it can verify rather
            than inventing prices, ratings or availability.
          </p>

          <div className="stays-links">
            <Link to="/explore" className="stays-secondary-link">
              Explore places <ArrowRight size={16} />
            </Link>
            <Link to="/planner" className="stays-secondary-link">
              Plan a journey <ArrowRight size={16} />
            </Link>
          </div>
        </div>

        <div className="stays-hero-art" aria-hidden="true">
          <div className="stays-orbit orbit-a" />
          <div className="stays-orbit orbit-b" />
          <div className="stays-orbit orbit-c" />
          <div className="stays-art-core">
            <Hotel size={38} strokeWidth={1.5} />
            <span>STAY</span>
          </div>
        </div>
      </section>

      <section className="stays-search-section">
        <div className="stays-search-head">
          <div>
            <span>01 / DESTINATION</span>
            <h2>Where are you staying?</h2>
          </div>
          <p>
            Choose a real geographic result first. Roamora then searches the
            selected coordinates and can fall back to another public map-data
            endpoint if one provider is unavailable.
          </p>
        </div>

        <div className="stays-search-card">
          <div className="stays-location-field">
            <MapPin size={18} />
            <div>
              <label htmlFor="stays-location">Destination</label>
              <input
                id="stays-location"
                value={query}
                placeholder="City, region or landmark"
                autoComplete="off"
                onChange={(event) => {
                  setQuery(event.target.value);
                  setSelectedLocation(null);
                  setSuggestions([]);
                  setSearched(false);
                  setPlaces([]);
                  setError("");
                }}
              />
            </div>
            <Search size={17} className="stays-field-search" />
          </div>

          {suggestions.length > 0 && query.trim().length >= 2 && !selectedLocation && (
            <div className="stays-suggestions">
              {suggestions.map((suggestion) => (
                <button
                  type="button"
                  key={`${suggestion.name}-${suggestion.latitude}-${suggestion.longitude}`}
                  onClick={() => {
                    setSelectedLocation(suggestion);
                    setQuery(
                      suggestion.country
                        ? `${suggestion.name}, ${suggestion.country}`
                        : suggestion.name
                    );
                    setSuggestions([]);
                    setError("");
                  }}
                >
                  <MapPin size={16} />
                  <span>
                    <strong>{suggestion.name}</strong>
                    <small>
                      {[
                        suggestion.country,
                        suggestion.latitude?.toFixed(3),
                        suggestion.longitude?.toFixed(3),
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </small>
                  </span>
                </button>
              ))}
            </div>
          )}

          {suggestionLoading && (
            <span className="stays-hint">Searching locations…</span>
          )}

          <div className="stays-controls">
            <div className="stays-filter-block">
              <span className="stays-control-label">Stay type</span>
              <div className="stays-filter-row">
                {stayFilters.map(({ label, value, icon: Icon }) => (
                  <button
                    type="button"
                    key={value}
                    className={category === value ? "active" : ""}
                    onClick={() => {
                      setCategory(value);
                      if (value !== "HOTEL") setStarFilter(0);
                      if (canSearch) runSearch(value);
                    }}
                  >
                    <Icon size={16} />
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {category === "HOTEL" && (
              <div className="stays-filter-block stays-star-block">
                <span className="stays-control-label">Star class</span>
                <div className="stays-star-row" role="group" aria-label="Minimum mapped hotel star class">
                  {starFilters.map((value) => (
                    <button
                      type="button"
                      key={value}
                      className={starFilter === value ? "active" : ""}
                      onClick={() => setStarFilter(value)}
                      aria-label={value === 0 ? "Any star class" : `Minimum ${value} star class`}
                      title={value === 0 ? "Any star class" : `Minimum ${value} star class`}
                    >
                      {value === 0 ? "Any" : `${value}★`}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <label className="stays-radius">
              <span>Search radius</span>
              <select value={radius} onChange={(event) => setRadius(event.target.value)}>
                <option value="3">3 km</option>
                <option value="8">8 km</option>
                <option value="15">15 km</option>
                <option value="25">25 km</option>
              </select>
            </label>

            <button
              type="button"
              className="stays-search-button"
              onClick={() => runSearch()}
              disabled={loading || (!canSearch && query.trim().length < 2)}
            >
              {loading ? "Searching…" : "Find stays"}
              <ArrowRight size={17} />
            </button>
          </div>
        </div>

        {error && <p className="stays-error">{error}</p>}
      </section>

      <section className="stays-results">
        <div className="stays-results-head">
          <div>
            <span>02 / NEARBY STAYS</span>
            <h2>
              {searched && selectedLocation
                ? `Stays around ${selectedLocation.name}`
                : "Discover your next base"}
            </h2>
          </div>
          {selectedLocation && (
            <div className="stays-location-chip">
              <Compass size={15} />
              {selectedLocation.name}
              {category === "HOTEL" && ` · ${starFilterLabel}`}
            </div>
          )}
        </div>

        {!searched && (
          <div className="stays-empty">
            <div className="stays-empty-icon"><Hotel size={25} /></div>
            <strong>Search a destination to begin.</strong>
            <span>
              Results come from OpenStreetMap nearby-place data. Prices, live
              room availability and consumer review scores are not presented
              until a licensed booking/review provider is connected.
            </span>
          </div>
        )}

        {searched && filteredPlaces.length > 0 && (
          <div className="stays-grid">
            {filteredPlaces.map((place) => (
              <article className="stay-card" key={place.id}>
                <div className="stay-card-top">
                  <div className="stay-icon">
                    <Hotel size={19} />
                  </div>
                  <span>{formatCategory(place.category)}</span>
                </div>

                <h3>{place.name}</h3>
                {category === "HOTEL" && <StarClass value={place.starRating} />}
                <p className="stay-distance">{formatDistance(place.distanceKm)}</p>
                <p className="stay-address">
                  {place.address || "Address not available in map data."}
                </p>

                <div className="stay-card-actions">
                  <a
                    href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
                      `${place.name} ${place.latitude},${place.longitude}`
                    )}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Open map <ExternalLink size={14} />
                  </a>
                  {place.website && (
                    <a
                      href={place.website}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Website <ExternalLink size={14} />
                    </a>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}

        {searched && !loading && !filteredPlaces.length && (
          <div className="stays-empty compact">
            <Star size={22} />
            <strong>Nothing matches this star filter.</strong>
            <span>
              This filter uses star classifications actually present in the map data.
              Choose “Any” or another stay type to see all mapped results.
            </span>
          </div>
        )}
      </section>
    </main>
  );
}

export default StaysPage;
