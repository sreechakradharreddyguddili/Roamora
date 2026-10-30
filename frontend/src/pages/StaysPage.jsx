import { useEffect, useMemo, useRef, useState } from "react";
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
import { Link, useSearchParams } from "react-router-dom";

import { resolveLocation, searchLocations } from "../api/routeApi";
import { getNearbyPlaces } from "../api/placeApi";

import "./StaysPage.css";
import { handleImageError } from "../utils/imageFallback";

const stayFilters = [
  { label: "Hotels", value: "HOTEL", icon: Hotel },
  { label: "Hostels", value: "HOSTEL", icon: BedDouble },
  { label: "Guest houses", value: "GUEST_HOUSE", icon: BedDouble },
  { label: "Resorts", value: "RESORT", icon: Sparkles },
];

const starFilters = [0, 1, 2, 3, 4, 5];

const indiaStateCities = {
  "Andhra Pradesh": ["Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Tirupati", "Kurnool", "Rajahmundry", "Kakinada", "Srikalahasti", "Kavali", "Ongole", "Bhimavaram", "Araku Valley", "Lepakshi"],
  "Arunachal Pradesh": ["Itanagar", "Naharlagun", "Pasighat"],
  "Assam": ["Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Tezpur"],
  "Bihar": ["Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga"],
  "Chhattisgarh": ["Raipur", "Bilaspur", "Durg", "Bhilai", "Jagdalpur"],
  "Goa": ["Panaji", "Margao", "Vasco da Gama", "Mapusa"],
  "Gujarat": ["Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Gandhinagar"],
  "Haryana": ["Gurugram", "Faridabad", "Panipat", "Hisar", "Ambala"],
  "Himachal Pradesh": ["Shimla", "Manali", "Dharamshala", "Solan", "Mandi", "Kasol", "Dalhousie", "Spiti Valley", "Kasauli"],
  "Jharkhand": ["Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar"],
  "Karnataka": ["Bengaluru", "Mysuru", "Mangaluru", "Hubballi", "Belagavi", "Shivamogga"],
  "Kerala": ["Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur", "Kollam", "Alappuzha", "Munnar", "Wayanad", "Varkala", "Thekkady", "Kumarakom"],
  "Madhya Pradesh": ["Indore", "Bhopal", "Jabalpur", "Gwalior", "Ujjain"],
  "Maharashtra": ["Mumbai", "Pune", "Nagpur", "Nashik", "Thane", "Chhatrapati Sambhajinagar"],
  "Manipur": ["Imphal", "Thoubal", "Bishnupur"],
  "Meghalaya": ["Shillong", "Tura", "Jowai"],
  "Mizoram": ["Aizawl", "Lunglei", "Champhai"],
  "Nagaland": ["Kohima", "Dimapur", "Mokokchung"],
  "Odisha": ["Bhubaneswar", "Cuttack", "Puri", "Rourkela", "Sambalpur"],
  "Punjab": ["Amritsar", "Ludhiana", "Jalandhar", "Patiala", "Bathinda"],
  "Rajasthan": ["Jaipur", "Udaipur", "Jodhpur", "Kota", "Ajmer", "Jaisalmer", "Pushkar", "Mount Abu", "Bundi", "Chittorgarh"],
  "Sikkim": ["Gangtok", "Namchi", "Gyalshing"],
  "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli"],
  "Telangana": ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam"],
  "Tripura": ["Agartala", "Udaipur", "Dharmanagar"],
  "Uttar Pradesh": ["Lucknow", "Varanasi", "Agra", "Kanpur", "Prayagraj", "Noida", "Ayodhya"],
  "Uttarakhand": ["Dehradun", "Haridwar", "Rishikesh", "Nainital", "Haldwani"],
  "West Bengal": ["Kolkata", "Siliguri", "Durgapur", "Asansol", "Darjeeling"],
  "Andaman and Nicobar Islands": ["Port Blair"],
  "Chandigarh": ["Chandigarh"],
  "Dadra and Nagar Haveli and Daman and Diu": ["Daman", "Silvassa"],
  "Delhi": ["New Delhi", "Dwarka", "Rohini"],
  "Jammu and Kashmir": ["Srinagar", "Jammu", "Anantnag"],
  "Ladakh": ["Leh", "Kargil"],
  "Lakshadweep": ["Kavaratti"],
  "Puducherry": ["Puducherry", "Karaikal"]
};


const internationalCountryCities = {
  "United States": ["New York", "Los Angeles", "Chicago", "San Francisco", "Miami", "Seattle", "Boston", "Las Vegas"],
  "United Kingdom": ["London", "Manchester", "Edinburgh", "Birmingham", "Liverpool", "Bristol", "Glasgow", "Oxford", "Bath", "York", "Cambridge", "Windermere"],
  "France": ["Paris", "Lyon", "Marseille", "Nice", "Bordeaux", "Toulouse", "Strasbourg", "Lille", "Annecy", "Avignon", "Colmar", "Chamonix"],
  "Italy": ["Rome", "Milan", "Florence", "Venice", "Naples", "Bologna", "Turin", "Verona", "Siena", "Como", "Cinque Terre"],
  "Spain": ["Madrid", "Barcelona", "Seville", "Valencia", "Malaga", "Granada", "Bilbao", "Palma"],
  "Germany": ["Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Dresden", "Stuttgart", "Heidelberg"],
  "Japan": ["Tokyo", "Kyoto", "Osaka", "Sapporo", "Fukuoka", "Nara", "Hiroshima", "Yokohama"],
  "Australia": ["Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Canberra", "Gold Coast", "Hobart"],
  "Canada": ["Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa", "Quebec City", "Edmonton", "Halifax", "Banff", "Whistler", "Victoria"],
  "United Arab Emirates": ["Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Al Ain", "Fujairah"],
  "Thailand": ["Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Krabi", "Ayutthaya", "Hua Hin"],
  "Indonesia": ["Jakarta", "Bali", "Yogyakarta", "Surabaya", "Bandung", "Lombok", "Medan"],
  "Singapore": ["Singapore"],
  "Portugal": ["Lisbon", "Porto", "Faro", "Coimbra", "Braga", "Funchal"],
  "Netherlands": ["Amsterdam", "Rotterdam", "Utrecht", "The Hague", "Eindhoven", "Maastricht"],
  "Switzerland": ["Zurich", "Geneva", "Lucerne", "Bern", "Basel", "Interlaken", "Lausanne"],
  "Greece": ["Athens", "Thessaloniki", "Santorini", "Mykonos", "Heraklion", "Rhodes"],
  "Turkey": ["Istanbul", "Ankara", "Izmir", "Antalya", "Bursa", "Cappadocia"],
  "Malaysia": ["Kuala Lumpur", "Penang", "Johor Bahru", "Malacca City", "Kota Kinabalu", "Kuching"],
  "South Korea": ["Seoul", "Busan", "Incheon", "Jeju City", "Daegu", "Gyeongju"],
  "China": ["Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Chengdu", "Hangzhou", "Xi'an"],
  "Brazil": ["São Paulo", "Rio de Janeiro", "Brasília", "Salvador", "Curitiba", "Florianópolis"],
  "Mexico": ["Mexico City", "Cancún", "Guadalajara", "Oaxaca", "Mérida", "Monterrey"],
  "South Africa": ["Cape Town", "Johannesburg", "Durban", "Pretoria", "Gqeberha", "Stellenbosch"],
  "New Zealand": ["Auckland", "Wellington", "Christchurch", "Queenstown", "Rotorua", "Dunedin"],
  "Ireland": ["Dublin", "Cork", "Galway", "Limerick", "Killarney", "Waterford"],
  "Sri Lanka": ["Colombo", "Kandy", "Galle", "Negombo", "Jaffna", "Ella", "Sigiriya"],
  "Nepal": ["Kathmandu", "Pokhara", "Lalitpur", "Bharatpur", "Biratnagar", "Lumbini"],
  "Maldives": ["Malé", "Hulhumalé", "Maafushi", "Addu City"],
  "Egypt": ["Cairo", "Alexandria", "Giza", "Luxor", "Aswan", "Sharm El Sheikh"],
  "Saudi Arabia": ["Riyadh", "Jeddah", "Mecca", "Medina", "Dammam", "AlUla"],
};

function locationLabel(location) {
  return [location?.name, location?.region, location?.country].filter(Boolean).join(", ");
}

function isCountryOnly(location) {
  return Boolean(location?.countryLevel) || Boolean(
    location?.name && location?.country &&
    location.name.trim().toLowerCase() === location.country.trim().toLowerCase() &&
    !location.region && !location.district
  );
}

function matchesLocationQuery(location, query) {
  if (!location?.name || !query) return false;
  const normalizedQuery = query.trim().toLowerCase();
  if (isCountryOnly(location)) {
    const countryNames = [location.name, location.country].filter(Boolean).map((value) => value.trim().toLowerCase());
    const parts = normalizedQuery.split(",").map((value) => value.trim()).filter(Boolean);
    // A country selection must not be mistaken for a city query just because
    // the typed city query also contains that country's name.
    return parts.length > 0 && parts.every((part) => countryNames.includes(part));
  }
  const normalizedName = location.name.trim().toLowerCase();
  const normalizedCountry = location.country?.trim().toLowerCase();
  return normalizedQuery.includes(normalizedName) &&
    (!normalizedQuery.includes(",") || !normalizedCountry || normalizedQuery.includes(normalizedCountry));
}

const stayPhotos = {
  HOTEL: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=82",
  HOSTEL: "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=900&q=82",
  GUEST_HOUSE: "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=900&q=82",
  RESORT: "https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=900&q=82",
};

function stayMapUrl(place) {
  const hasCoordinates = place.latitude !== null && place.latitude !== undefined && place.longitude !== null && place.longitude !== undefined && String(place.latitude).trim() !== "" && String(place.longitude).trim() !== "" && Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude));
  const query = hasCoordinates ? `${place.latitude},${place.longitude}` : [place.name, place.address].filter(Boolean).join(" ") || "hotel";
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

function placePhotoUrl(place, fallback) {
  const value = typeof place?.imageUrl === "string" ? place.imageUrl.trim() : "";
  if (/^https?:\/\//i.test(value)) return value;
  if (/^File:/i.test(value)) return `https://commons.wikimedia.org/wiki/Special:FilePath/${encodeURIComponent(value.replace(/^File:/i, "").trim())}`;
  return fallback;
}

function safeWebsite(value) {
  if (typeof value !== "string" || !value.trim()) return null;
  try {
    const candidate = /^https?:\/\//i.test(value) ? value : `https://${value}`;
    const parsed = new URL(candidate);
    return ["https:", "http:"].includes(parsed.protocol) ? parsed.href : null;
  } catch {
    return null;
  }
}

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
  if (value === null || value === undefined || String(value).trim() === "") {
    return <span className="stay-star-unknown">Star class not listed</span>;
  }
  const rating = Number(value);

  if (!Number.isFinite(rating) || rating < 1 || rating > 5) {
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
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("destination") || "");
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [selectedIndiaState, setSelectedIndiaState] = useState("Andhra Pradesh");
  const [internationalCity, setInternationalCity] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [category, setCategory] = useState("HOTEL");
  const [radius, setRadius] = useState("8");
  const [starFilter, setStarFilter] = useState(0);
  const [places, setPlaces] = useState([]);
  const [loading, setLoading] = useState(false);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState("");
  const autoSearchKey = useRef("");
  const runSearchRef = useRef(null);
  const searchSequence = useRef(0);

  useEffect(() => {
    const initialDestination = searchParams.get("destination");
    if (initialDestination) {
      // URL search params are external navigation state used by the copilot and Explore links.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setQuery(initialDestination);
      setSelectedLocation(null);
      setPlaces([]);
      setSearched(false);
    }
  }, [searchParams]);

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
    }, 180);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [query, selectedLocation]);

  const canSearch = Boolean(
    selectedLocation?.latitude !== undefined &&
      selectedLocation?.longitude !== undefined &&
      !isCountryOnly(selectedLocation)
  );
  const showIndiaBrowser = /\bindia\b/i.test(query) && (!selectedLocation || isCountryOnly(selectedLocation));
  const selectedCountry = selectedLocation?.country || selectedLocation?.name || query.split(",").at(-1)?.trim() || "";
  const showInternationalBrowser = Boolean(selectedLocation && isCountryOnly(selectedLocation) && !/\bindia\b/i.test(selectedCountry));
  const internationalCities = internationalCountryCities[selectedCountry] || internationalCountryCities[Object.keys(internationalCountryCities).find((country) => country.toLowerCase() === selectedCountry.toLowerCase())] || [];

  async function runSearch(nextCategory = category, queryOverride = query) {
    const requestId = ++searchSequence.current;
    let location = selectedLocation;
    if (queryOverride && location && !matchesLocationQuery(location, queryOverride)) location = null;

    if (!location || location.latitude === undefined || location.longitude === undefined) {
      if (queryOverride.trim().length < 2) {
        setError("Enter a destination first.");
        return;
      }

      try {
        setSuggestionLoading(true);
        location = await resolveLocation(queryOverride);
        if (requestId !== searchSequence.current) return;
        setSelectedLocation(location);
        setQuery(locationLabel(location));
        setSuggestions([]);
      } catch (requestError) {
        if (requestId !== searchSequence.current) return;
        setError(
          requestError?.message ||
            "We could not resolve that destination. Try a city or landmark name."
        );
        return;
      } finally {
        if (requestId === searchSequence.current) setSuggestionLoading(false);
      }
    }

    if (requestId !== searchSequence.current) return;
    if (isCountryOnly(location)) {
      setSelectedLocation(location);
      setError("");
      setSearched(false);
      setPlaces([]);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const searchOptions = {
        latitude: location.latitude,
        longitude: location.longitude,
        category: nextCategory,
        limit: 80,
      };
      // Run the selected radius and broad-radius fallback concurrently so an empty
      // small-radius result does not force users to wait through serial timeouts.
      const radiusChoices = [...new Set([Number(radius), 25])];
      const settledResults = await Promise.allSettled(radiusChoices.map((radiusKm) => getNearbyPlaces({ ...searchOptions, radiusKm })));
      if (requestId !== searchSequence.current) return;
      const resultSets = settledResults.filter((item) => item.status === "fulfilled").map((item) => item.value);
      if (!resultSets.length) throw settledResults.find((item) => item.status === "rejected")?.reason || new Error("Nearby stay information is temporarily unavailable.");
      let result = resultSets.find((items) => items.length) || [];


      // Prefer well-described and higher-class listings, then nearer options.
      result = [...result].sort((a, b) => {
        const aRating = Number(a.rating ?? a.reviewRating ?? a.userRating);
        const bRating = Number(b.rating ?? b.reviewRating ?? b.userRating);
        const aHasRating = Number.isFinite(aRating) && aRating >= 0 && aRating <= 5;
        const bHasRating = Number.isFinite(bRating) && bRating >= 0 && bRating <= 5;
        const aStars = Number(a.starRating) || 0;
        const bStars = Number(b.starRating) || 0;
        const aNamed = a.name ? 1 : 0;
        const bNamed = b.name ? 1 : 0;
        return (Number(bHasRating) - Number(aHasRating)) || ((bHasRating && aHasRating) ? bRating - aRating : 0) || (bNamed - aNamed) || (bStars - aStars) || ((Number(a.distanceKm) || 9999) - (Number(b.distanceKm) || 9999));
      });
      setPlaces(result);
      setSearched(true);

      if (!result.length) {
        setError("No mapped stays were returned for this area. Try another stay type or open the destination in Google Maps to see current accommodation listings.");
      }
    } catch (requestError) {
      if (requestId !== searchSequence.current) return;
      setPlaces([]);
      setSearched(true);
      setError(
        requestError?.message ||
          "Nearby stay information is temporarily unavailable."
      );
    } finally {
      if (requestId === searchSequence.current) setLoading(false);
    }
  }

  useEffect(() => { runSearchRef.current = runSearch; });

  useEffect(() => {
    if (searchParams.get("autoSearch") !== "1") return;
    const key = searchParams.get("destination") || "";
    if (!key || autoSearchKey.current === key) return;
    autoSearchKey.current = key;
    runSearchRef.current?.(category, key);
  }, [searchParams, category]);

  const filteredPlaces = useMemo(() => {
    if (starFilter === 0) return places.slice(0, 30);
    return places.filter((place) => {
      const raw = place.rating ?? place.reviewRating ?? place.userRating ?? place.starRating;
      const rating = Number(raw);
      return raw !== null && raw !== undefined && String(raw).trim() !== "" && Number.isFinite(rating) && rating >= starFilter && rating <= 5;
    }).slice(0, 30);
  }, [places, starFilter]);

  const starFilterLabel = starFilter === 0 ? "Any rating" : `${starFilter}★+`;

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

        <div className="stays-hero-art">
          <img src="https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=85" alt="A welcoming hotel pool and accommodation setting" fetchPriority="high" onError={handleImageError} />
          <div className="stays-hero-photo-caption"><Hotel size={16} /> Find a stay near where you want to be.</div>
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
                    setQuery(locationLabel(suggestion));
                    setSuggestions([]);
                    setError("");
                  }}
                >
                  <MapPin size={16} />
                  <span>
                    <strong>{suggestion.name}</strong>
                    <small>
                      {[
                        suggestion.region,
                        suggestion.district,
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

          {showIndiaBrowser && (
            <div className="india-location-browser">
              <div className="india-browser-heading">
                <div><strong>Browse India by state and city</strong><small>Choose a city to search real nearby accommodation listings.</small></div>
                <span>INDIA</span>
              </div>
              <label className="india-state-select">State or union territory
                <select value={selectedIndiaState} onChange={(event) => setSelectedIndiaState(event.target.value)}>
                  {Object.keys(indiaStateCities).sort((a, b) => a.localeCompare(b)).map((state) => <option key={state} value={state}>{state}</option>)}
                </select>
              </label>
              <div className="india-city-list">
                {(indiaStateCities[selectedIndiaState] || []).map((city) => (
                  <button key={city} type="button" onClick={() => { const cityQuery = `${city}, ${selectedIndiaState}, India`; setQuery(cityQuery); setSelectedLocation(null); setSuggestions([]); setError(""); runSearch(category, cityQuery); }}>
                    <MapPin size={13}/>{city}<ArrowRight size={13}/>
                  </button>
                ))}
              </div>
            </div>
          )}

          {showInternationalBrowser && (
            <div className="india-location-browser international-location-browser">
              <div className="india-browser-heading">
                <div><strong>Browse {selectedCountry} by city</strong><small>Choose a city or town, or enter any other destination in this country.</small></div>
                <span>WORLDWIDE</span>
              </div>
              <form className="international-city-search" onSubmit={(event) => {
                event.preventDefault();
                const city = internationalCity.trim();
                if (!city) return;
                const cityQuery = `${city}, ${selectedCountry}`;
                setQuery(cityQuery); setSelectedLocation(null); setSuggestions([]); setError("");
                runSearch(category, cityQuery);
              }}>
                <input aria-label={`City in ${selectedCountry}`} value={internationalCity} onChange={(event) => setInternationalCity(event.target.value)} placeholder={`Enter a city or town in ${selectedCountry}`} />
                <button type="submit" disabled={!internationalCity.trim()}><Search size={15}/> Find stays</button>
              </form>
              {internationalCities.length > 0 && <div className="india-city-list">
                {internationalCities.map((city) => (
                  <button key={city} type="button" onClick={() => { const cityQuery = `${city}, ${selectedCountry}`; setQuery(cityQuery); setSelectedLocation(null); setSuggestions([]); setError(""); runSearch(category, cityQuery); }}>
                    <MapPin size={13}/>{city}<ArrowRight size={13}/>
                  </button>
                ))}
              </div>}
              <p className="international-browser-note">Other cities and towns are supported too—enter the name above. Listings depend on the map data available for that location.</p>
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

            {(category === "HOTEL" || category === "RESORT" || category === "HOSTEL" || category === "GUEST_HOUSE") && (
              <div className="stays-filter-block stays-star-block">
                <span className="stays-control-label">Minimum listed rating / class</span>
                <div className="stays-star-row" role="group" aria-label="Minimum listed rating or accommodation class">
                  {starFilters.map((value) => (
                    <button
                      type="button"
                      key={value}
                      className={starFilter === value ? "active" : ""}
                      onClick={() => setStarFilter(value)}
                      aria-label={value === 0 ? "Any rating" : `Minimum ${value} star rating or class`}
                      title={value === 0 ? "Any rating" : `Minimum ${value} star rating or class`}
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

        {error && <div className="stays-error" role="status"><span>{error}</span>{selectedLocation && !isCountryOnly(selectedLocation) && searched && !places.length && <a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${category.replaceAll("_", " ").toLowerCase()} near ${locationLabel(selectedLocation)}`)}`} target="_blank" rel="noopener noreferrer">Search live map listings <ExternalLink size={14}/></a>}</div>}
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
              {starFilter > 0 && ` · ${starFilterLabel}`}
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
              <article className="stay-card" key={place.id || `${place.name}-${place.latitude}-${place.longitude}`}>
                <div className="stay-card-photo">
                  <img src={placePhotoUrl(place, stayPhotos[place.category] || stayPhotos.HOTEL)} alt={`${place.name} accommodation photo`} loading="lazy" onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = stayPhotos[place.category] || stayPhotos.HOTEL; }} />
                  <span>{place.imageUrl ? "Place photo" : "Representative photo"}</span>
                </div>
                <div className="stay-card-top">
                  <div className="stay-icon">
                    <Hotel size={19} />
                  </div>
                  <span>{formatCategory(place.category)}</span>
                </div>

                <h3>{place.name}</h3>
                <StarClass value={place.starRating} />
                <p className="stay-review-rating">{Number.isFinite(Number(place.rating ?? place.reviewRating ?? place.userRating)) && String(place.rating ?? place.reviewRating ?? place.userRating ?? "").trim() !== "" && Number(place.rating ?? place.reviewRating ?? place.userRating) >= 0 && Number(place.rating ?? place.reviewRating ?? place.userRating) <= 5 ? `Customer rating: ${Number(place.rating ?? place.reviewRating ?? place.userRating).toFixed(1)} / 5` : "Customer review rating not supplied by map source"}</p>
                <p className="stay-price">{place.priceInfo ? `Listed price / fee: ${place.priceInfo}` : "Price not listed by map source"}</p>
                <p className="stay-distance">{formatDistance(place.distanceKm)}</p>
                <p className="stay-address">
                  {place.address || "Address not available in map data."}
                </p>

                <div className="stay-card-actions">
                  <a
                    href={stayMapUrl(place)}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Open map <ExternalLink size={14} />
                  </a>
                  {safeWebsite(place.website) ? (
                    <a href={safeWebsite(place.website)} target="_blank" rel="noopener noreferrer">Official website <ExternalLink size={14} /></a>
                  ) : (
                    <a href={`https://www.google.com/search?q=${encodeURIComponent(`${place.name} ${locationLabel(selectedLocation)} official website`)}`} target="_blank" rel="noopener noreferrer">Find website <ExternalLink size={14} /></a>
                  )}
                  <a href={`https://www.google.com/search?q=${encodeURIComponent(`${place.name} ${locationLabel(selectedLocation)} price booking`)}`} target="_blank" rel="noopener noreferrer">Check prices <ExternalLink size={14} /></a>
                </div>
              </article>
            ))}
          </div>
        )}

        {searched && !loading && places.length > 0 && !filteredPlaces.length && (
          <div className="stays-empty compact">
            <Star size={22} />
            <strong>Nothing matches this star filter.</strong>
            <span>
              This filter uses ratings or accommodation classes actually supplied by the map source. Unrated listings cannot be matched to a minimum rating. Choose “Any” to include every mapped listing.
            </span>
          </div>
        )}
      </section>
    </main>
  );
}

export default StaysPage;
