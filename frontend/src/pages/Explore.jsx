import {
  ArrowRight,
  CloudSun,
  ShieldCheck,
  Navigation,
  BarChart3,
  Route as RouteIcon,
  Compass,
  Hotel,
  MapPin,
  Search,
  Utensils,
  Landmark,
  Coffee,
} from "lucide-react";

import { Link } from "react-router-dom";
import { useEffect, useState } from "react";

import { resolveLocation, searchLocations } from "../api/routeApi";
import { getNearbyPlaces } from "../api/placeApi";

import "./Explore.css";

const categories = [
  "All",
  "Weather",
  "Risk",
  "Routes",
  "Decisions",
];

const cards = [
  {
    id: 1,
    category: "Weather",
    number: "01",
    title: "Weather along your route",
    description:
      "See real conditions across the important points of your journey.",
    icon: CloudSun,
    visual: "weather",
    link: "/planner?focus=weather",
    action: "Explore weather",
  },
  {
    id: 2,
    category: "Routes",
    number: "02",
    title: "Route comparisons",
    description:
      "Compare multiple routes using time, distance and risk.",
    icon: RouteIcon,
    visual: "routes",
    link: "/planner?focus=routes",
    action: "Compare routes",
  },
  {
    id: 3,
    category: "Risk",
    number: "03",
    title: "Risk intelligence",
    description:
      "Turn complex journey conditions into a simple signal.",
    icon: ShieldCheck,
    visual: "risk",
    link: "/planner?focus=risk",
    action: "View risk",
  },
  {
    id: 4,
    category: "Routes",
    number: "04",
    title: "Route visualization",
    description:
      "See the journey, not just the destination.",
    icon: Navigation,
    visual: "visualization",
    link: "/planner?focus=route-map",
    action: "View route",
  },
  {
    id: 5,
    category: "Decisions",
    number: "05",
    title: "Better decisions",
    description:
      "Make informed choices with all the important context in one place.",
    icon: BarChart3,
    visual: "decisions",
    link: "/planner?focus=decisions",
    action: "Make a decision",
  },
  {
    id: 6,
    category: "Decisions",
    number: "06",
    title: "Plan your journey",
    description:
      "Put everything together and start planning with confidence.",
    icon: Compass,
    visual: "planning",
    link: "/planner",
    action: "Start planning",
  },
];

function Visual({ type }) {
  if (type === "weather") {
    return (
      <div className="explore-visual explore-weather">
        <div className="weather-sun" />
        <div className="weather-orbit weather-orbit-one" />
        <div className="weather-orbit weather-orbit-two" />
        <div className="weather-temp">28°</div>

        <div className="weather-cloud">
          <span />
          <span />
        </div>
      </div>
    );
  }

  if (type === "routes") {
    return (
      <div className="explore-visual explore-routes">
        <div className="route-road route-road-one" />
        <div className="route-road route-road-two" />

        <svg
          className="route-line"
          viewBox="0 0 500 220"
          preserveAspectRatio="none"
        >
          <path
            d="M15 175 C100 160 95 70 190 80 C270 90 285 155 360 125 C410 105 440 70 485 35"
          />
        </svg>

        <div className="route-point route-start" />
        <div className="route-point route-end" />
      </div>
    );
  }

  if (type === "risk") {
    return (
      <div className="explore-visual explore-risk">
        <div className="risk-ring ring-one" />
        <div className="risk-ring ring-two" />
        <div className="risk-ring ring-three" />

        <div className="risk-core">
          <strong>LOW</strong>
          <span>RISK</span>
        </div>
      </div>
    );
  }

  if (type === "visualization") {
    return (
      <div className="explore-visual explore-map">
        <svg
          className="map-route"
          viewBox="0 0 500 220"
          preserveAspectRatio="none"
        >
          <path
            d="M30 175 C120 175 105 75 205 85 C295 95 275 180 360 120 C410 85 445 65 475 35"
          />
        </svg>

        <div className="map-point map-point-start" />
        <div className="map-point map-point-end" />
      </div>
    );
  }

  if (type === "decisions") {
    return (
      <div className="explore-visual explore-decisions">
        <div className="decision-bar bar-one" />
        <div className="decision-bar bar-two" />
        <div className="decision-bar bar-three" />
        <div className="decision-bar bar-four" />
      </div>
    );
  }

  return (
    <div className="explore-visual explore-planning">
      <div className="planning-ring planning-ring-one" />
      <div className="planning-ring planning-ring-two" />

      <div className="planning-center">
        <Compass size={34} strokeWidth={1.8} />
      </div>
    </div>
  );
}

export default function Explore() {
  const [activeCategory, setActiveCategory] = useState("All");
  const [destination, setDestination] = useState("");
  const [selectedDestination, setSelectedDestination] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [placeFilter, setPlaceFilter] = useState("");
  const [places, setPlaces] = useState([]);
  const [placesLoading, setPlacesLoading] = useState(false);
  const [placesError, setPlacesError] = useState("");

  useEffect(() => {
    const value = destination.trim();
    if (value.length < 2 || selectedDestination) {
      return undefined;
    }

    let active = true;
    const timer = window.setTimeout(async () => {
      try {
        const result = await searchLocations(value);
        if (active) setSuggestions(result.slice(0, 5));
      } catch {
        if (active) setSuggestions([]);
      }
    }, 260);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [destination, selectedDestination]);

  async function discoverPlaces(category = placeFilter) {
    let location = selectedDestination;
    setPlacesLoading(true);
    setPlacesError("");

    try {
      if (!location || location.latitude === undefined || location.longitude === undefined) {
        if (destination.trim().length < 2) {
          throw new Error("Enter a destination first.");
        }
        location = await resolveLocation(destination);
        setSelectedDestination(location);
        setDestination(
          location.country
            ? `${location.name}, ${location.country}`
            : location.name
        );
        setSuggestions([]);
      }

      const result = await getNearbyPlaces({
        latitude: location.latitude,
        longitude: location.longitude,
        category: category || undefined,
        radiusKm: 10,
        limit: 30,
      });
      setPlaces(result);
    } catch (error) {
      setPlaces([]);
      setPlacesError(error?.message || "Nearby places are temporarily unavailable.");
    } finally {
      setPlacesLoading(false);
    }
  }

  const filteredCards =
    activeCategory === "All"
      ? cards
      : cards.filter(
          (card) => card.category === activeCategory
        );

  return (
    <main className="explore-page">

      {/* HERO */}
      <section className="explore-hero">
        <div className="explore-hero-content">
          <span className="explore-eyebrow">
            ROAMORA / EXPLORE
          </span>

          <h1>
            Understand the journey
            <span> before you take it.</span>
          </h1>

          <p>
            Explore how Roamora combines weather, routes and
            travel intelligence into one planning experience.
          </p>

          <Link
            to="/planner"
            className="explore-primary-button"
          >
            Start planning
            <ArrowRight size={18} />
          </Link>
        </div>

        <div className="explore-hero-orbit">
          <div className="hero-orbit-line orbit-one" />
          <div className="hero-orbit-line orbit-two" />
          <div className="hero-orbit-line orbit-three" />

          <div className="hero-orbit-center">
            <Compass size={46} strokeWidth={1.5} />
          </div>
        </div>
      </section>

      {/* REAL DESTINATION DISCOVERY */}
      <section className="explore-discovery">
        <div className="explore-discovery-heading">
          <div>
            <span className="explore-eyebrow">ROAMORA / DESTINATION DISCOVERY</span>
            <h2>Explore what is actually <em>near you.</em></h2>
          </div>
          <p>
            Search a destination and discover mapped stays, attractions,
            restaurants and useful travel places. Availability, ratings and
            prices are not invented when a provider is not connected.
          </p>
        </div>

        <div className="explore-discovery-search">
          <div className="explore-location-input">
            <MapPin size={18} />
            <input
              value={destination}
              placeholder="Try London, Tokyo, Paris, Hyderabad…"
              onChange={(event) => {
                setDestination(event.target.value);
                setSelectedDestination(null);
                setPlaces([]);
                setPlacesError("");
              }}
            />
            <Search size={17} />
          </div>

          {suggestions.length > 0 && destination.trim().length >= 2 && !selectedDestination && (
            <div className="explore-suggestions">
              {suggestions.map((suggestion) => (
                <button
                  type="button"
                  key={`${suggestion.name}-${suggestion.latitude}-${suggestion.longitude}`}
                  onClick={() => {
                    setSelectedDestination(suggestion);
                    setDestination(
                      suggestion.country
                        ? `${suggestion.name}, ${suggestion.country}`
                        : suggestion.name
                    );
                    setSuggestions([]);
                    setPlacesError("");
                  }}
                >
                  <MapPin size={15} />
                  <span>
                    <strong>{suggestion.name}</strong>
                    <small>{suggestion.country || "Worldwide location"}</small>
                  </span>
                </button>
              ))}
            </div>
          )}

          <div className="explore-discovery-filters">
            {[
              ["", "Everything", Compass],
              ["HOTEL", "Hotels", Hotel],
              ["ATTRACTION", "Attractions", Landmark],
              ["RESTAURANT", "Restaurants", Utensils],
              ["CAFE", "Cafés", Coffee],
            ].map(([value, label, Icon]) => (
              <button
                type="button"
                key={label}
                className={placeFilter === value ? "active" : ""}
                onClick={() => {
                  setPlaceFilter(value);
                  if (selectedDestination) discoverPlaces(value);
                }}
              >
                <Icon size={15} />
                {label}
              </button>
            ))}

            <button
              type="button"
              className="explore-discover-button"
              disabled={placesLoading || (!selectedDestination && destination.trim().length < 2)}
              onClick={() => discoverPlaces()}
            >
              {placesLoading ? "Discovering…" : "Discover nearby"}
              <ArrowRight size={16} />
            </button>
          </div>
        </div>

        {placesError && (
          <div className="explore-discovery-empty explore-discovery-error">
            {placesError}
          </div>
        )}

        {places.length > 0 && (
          <div className="explore-place-grid">
            {places.slice(0, 9).map((place) => (
              <article className="explore-place-card" key={place.id}>
                <div className="explore-place-icon">
                  {place.category === "HOTEL" ? <Hotel size={18} /> : <Landmark size={18} />}
                </div>
                <span>{place.category?.replaceAll("_", " ")}</span>
                <h3>{place.name}</h3>
                <p>
                  {place.distanceKm != null
                    ? `${Number(place.distanceKm).toFixed(1)} km away`
                    : "Nearby"}
                </p>
                <a
                  href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
                    `${place.name} ${place.latitude},${place.longitude}`
                  )}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  Open map <ArrowRight size={14} />
                </a>
              </article>
            ))}
          </div>
        )}

        {selectedDestination && !places.length && !placesLoading && !placesError && (
          <div className="explore-discovery-empty">
            Select a category or discover nearby places to populate the destination layer.
          </div>
        )}
      </section>

      {/* FILTERS */}
      <section className="explore-content">

        <div className="explore-filters">
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              className={
                activeCategory === category
                  ? "explore-filter active"
                  : "explore-filter"
              }
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>

        {/* CARDS */}
        <div
          className="explore-grid"
          key={activeCategory}
        >
          {filteredCards.map((card) => {
            const Icon = card.icon;

            return (
              <Link
                key={card.id}
                to={card.link}
                className="explore-card-link"
              >
                <article className="explore-card">

                  <Visual type={card.visual} />

                  <div className="explore-card-content">

                    <div className="explore-card-top">
                      <span className="explore-number">
                        {card.number}
                      </span>

                      <Icon
                        className="explore-card-icon"
                        size={21}
                        strokeWidth={1.8}
                      />
                    </div>

                    <span className="explore-category">
                      {card.category}
                    </span>

                    <h2>{card.title}</h2>

                    <p>{card.description}</p>

                    <div className="explore-card-action">
                      {card.action}
                      <ArrowRight size={17} />
                    </div>

                  </div>
                </article>
              </Link>
            );
          })}
        </div>

      </section>

      {/* CTA */}
      <section className="explore-bottom-cta">
        <div>
          <span className="explore-eyebrow">
            READY WHEN YOU ARE
          </span>

          <h2>
            Your next journey
            <span> starts here.</span>
          </h2>
        </div>

        <Link
          to="/planner"
          className="explore-cta-button"
        >
          Open planner
          <ArrowRight size={18} />
        </Link>
      </section>

    </main>
  );
}