import { useEffect, useRef, useState } from "react";
import { ArrowRight, Coffee, Hotel, Landmark, MapPin, Search, Utensils, Compass, ExternalLink, LocateFixed } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { resolveLocation, searchLocations } from "../api/routeApi";
import { getNearbyPlaces } from "../api/placeApi";
import "./Explore.css";
import { handleImageError } from "../utils/imageFallback";

const destinations = [
  { name: "Tokyo", country: "Japan", image: "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1000&q=82", alt: "Tokyo skyline and city lights" },
  { name: "Rome", country: "Italy", image: "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1000&q=82", alt: "Historic Rome architecture" },
  { name: "Jaipur", country: "India", image: "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=1000&q=82", alt: "Pink sandstone palace architecture in Rajasthan" },
  { name: "Bali", country: "Indonesia", image: "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1000&q=82", alt: "Tropical Bali landscape" },
  { name: "London", country: "United Kingdom", image: "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1000&q=82", alt: "London city skyline and historic architecture" },
  { name: "New York", country: "United States", image: "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1000&q=82", alt: "New York city buildings" },
  { name: "Lisbon", country: "Portugal", image: "https://images.unsplash.com/photo-1555881400-74d7acaacd8b?auto=format&fit=crop&w=1000&q=82", alt: "Lisbon streets and colourful buildings" },
  { name: "Singapore", country: "Singapore", image: "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=1000&q=82", alt: "Singapore skyline and waterfront" },
];
const countryCityTowns = {
  "India": ["Nellore", "Tirupati", "Visakhapatnam", "Vijayawada", "Hyderabad", "Bengaluru", "Chennai", "Mysuru", "Kochi", "Puducherry", "Ooty", "Munnar", "Hampi", "Rishikesh", "Manali", "Darjeeling"],
  "United States": ["New York", "Los Angeles", "Chicago", "San Francisco", "Miami", "Seattle", "Boston", "Sedona", "Asheville", "Key West"],
  "United Kingdom": ["London", "Manchester", "Edinburgh", "Birmingham", "Liverpool", "Oxford", "Bath", "York", "Cambridge", "Windermere"],
  "France": ["Paris", "Lyon", "Marseille", "Nice", "Bordeaux", "Toulouse", "Annecy", "Avignon", "Colmar", "Chamonix"],
  "Italy": ["Rome", "Milan", "Florence", "Venice", "Naples", "Bologna", "Verona", "Siena", "Cinque Terre", "Como"],
  "Spain": ["Madrid", "Barcelona", "Seville", "Valencia", "Malaga", "Granada", "Toledo", "Ronda", "Bilbao"],
  "Germany": ["Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Heidelberg", "Rothenburg ob der Tauber", "Freiburg"],
  "Japan": ["Tokyo", "Kyoto", "Osaka", "Sapporo", "Nara", "Hiroshima", "Hakone", "Kanazawa", "Takayama"],
  "Australia": ["Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Cairns", "Byron Bay", "Hobart"],
  "Canada": ["Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa", "Quebec City", "Banff", "Victoria", "Whistler"],
  "United Arab Emirates": ["Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Al Ain", "Fujairah"],
  "Thailand": ["Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Krabi", "Ayutthaya", "Pai", "Hua Hin"],
  "Indonesia": ["Jakarta", "Bali", "Yogyakarta", "Surabaya", "Bandung", "Lombok", "Ubud", "Seminyak"],
  "Portugal": ["Lisbon", "Porto", "Faro", "Coimbra", "Braga", "Sintra", "Évora", "Lagos"],
  "Netherlands": ["Amsterdam", "Rotterdam", "Utrecht", "The Hague", "Eindhoven", "Delft", "Leiden"],
  "Switzerland": ["Zurich", "Geneva", "Lucerne", "Bern", "Basel", "Interlaken", "Zermatt", "Lauterbrunnen"],
  "Greece": ["Athens", "Thessaloniki", "Santorini", "Mykonos", "Heraklion", "Rhodes", "Nafplio"],
  "Turkey": ["Istanbul", "Ankara", "Izmir", "Antalya", "Bursa", "Cappadocia", "Fethiye"],
  "Singapore": ["Singapore"], "Nepal": ["Kathmandu", "Pokhara", "Lalitpur", "Bhaktapur", "Lumbini", "Nagarkot"],
  "Malaysia": ["Kuala Lumpur", "Penang", "Johor Bahru", "Malacca City", "Kota Kinabalu", "Kuching", "Langkawi"],
  "South Korea": ["Seoul", "Busan", "Incheon", "Jeju City", "Daegu", "Gyeongju", "Suwon"],
  "China": ["Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Chengdu", "Hangzhou", "Xi'an", "Suzhou"],
  "Brazil": ["São Paulo", "Rio de Janeiro", "Brasília", "Salvador", "Curitiba", "Florianópolis", "Foz do Iguaçu"],
  "Mexico": ["Mexico City", "Cancún", "Guadalajara", "Oaxaca", "Mérida", "Monterrey", "Playa del Carmen"],
  "Egypt": ["Cairo", "Alexandria", "Giza", "Luxor", "Aswan", "Sharm El Sheikh"],
  "Saudi Arabia": ["Riyadh", "Jeddah", "Mecca", "Medina", "Dammam", "AlUla"],
  "Maldives": ["Malé", "Hulhumalé", "Maafushi", "Addu City"],
  "United Arab Emirates": ["Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Al Ain", "Fujairah"],
  "Sri Lanka": ["Colombo", "Kandy", "Galle", "Negombo", "Jaffna", "Ella", "Sigiriya", "Mirissa"],
  "New Zealand": ["Auckland", "Wellington", "Christchurch", "Queenstown", "Rotorua", "Dunedin", "Taupō"],
  "South Africa": ["Cape Town", "Johannesburg", "Durban", "Pretoria", "Stellenbosch", "Knysna"],
  "Ireland": ["Dublin", "Cork", "Galway", "Limerick", "Killarney", "Waterford"],
};
const foodPhotos = [
  { title: "Find local favourites", text: "Search mapped restaurants around your destination.", image: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=82", alt: "Welcoming restaurant interior", category: "RESTAURANT" },
  { title: "Take a coffee break", text: "Discover cafés for a slower moment on your trip.", image: "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=900&q=82", alt: "Coffee shop interior", category: "CAFE" },
  { title: "Taste something new", text: "Use nearby search to find food places on the map.", image: "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=82", alt: "A table of freshly prepared food", category: "RESTAURANT" },
];
const filters = [
  { value: "", label: "All places", icon: Compass },
  { value: "RESTAURANT", label: "Food", icon: Utensils },
  { value: "CAFE", label: "Cafés", icon: Coffee },
  { value: "HOTEL", label: "Stays", icon: Hotel },
  { value: "ATTRACTION", label: "Things to see", icon: Landmark },
];
const fallbackPhoto = {
  RESTAURANT: foodPhotos[0].image,
  CAFE: foodPhotos[1].image,
  HOTEL: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=82",
  HOSTEL: "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=900&q=82",
  GUEST_HOUSE: "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=900&q=82",
  GUESTHOUSE: "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=900&q=82",
  RESORT: "https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=900&q=82",
  ATTRACTION: destinations[1].image,
};
function locationLabel(location) { return [location?.name, location?.region, location?.country].filter(Boolean).join(", "); }
function isCountryOnly(location) { return Boolean(location?.countryLevel) || Boolean(location?.name && location?.country && location.name.trim().toLowerCase() === location.country.trim().toLowerCase() && !location.region && !location.district); }
function matchesLocationQuery(location, query) { if (!location?.name || !query) return false; const q = query.trim().toLowerCase(); if (isCountryOnly(location)) { const names = [location.name, location.country].filter(Boolean).map((value) => value.trim().toLowerCase()); const parts = q.split(",").map((value) => value.trim()).filter(Boolean); return parts.length > 0 && parts.every((part) => names.includes(part)); } const name = location.name.trim().toLowerCase(); const country = location.country?.trim().toLowerCase(); return q.includes(name) && (!q.includes(",") || !country || q.includes(country)); }
function safeExternalUrl(value) {
  if (typeof value !== "string" || !value.trim()) return null;
  try { const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`); return ["https:", "http:"].includes(url.protocol) ? url.href : null; } catch { return null; }
}
function placePhotoUrl(place, fallback) {
  const value = typeof place?.imageUrl === "string" ? place.imageUrl.trim() : "";
  if (/^https?:\/\//i.test(value)) return value;
  if (/^File:/i.test(value)) return `https://commons.wikimedia.org/wiki/Special:FilePath/${encodeURIComponent(value.replace(/^File:/i, "").trim())}`;
  return fallback;
}
function mapUrl(place) {
  const hasCoordinates = place.latitude !== null && place.latitude !== undefined && place.longitude !== null && place.longitude !== undefined && String(place.latitude).trim() !== "" && String(place.longitude).trim() !== "" && Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude));
  const query = hasCoordinates ? `${place.latitude},${place.longitude}` : [place.name, place.address].filter(Boolean).join(" ");
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

export default function Explore() {
  const [searchParams] = useSearchParams();
  const [destination, setDestination] = useState(searchParams.get("destination") || "");
  const [selectedDestination, setSelectedDestination] = useState(null);
  const [countryPlaceInput, setCountryPlaceInput] = useState("");
  const [suggestions, setSuggestions] = useState([]);
  const [placeFilter, setPlaceFilter] = useState(searchParams.get("category") || "");
  const [places, setPlaces] = useState([]);
  const [placesLoading, setPlacesLoading] = useState(false);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [placesError, setPlacesError] = useState("");
  const autoSearchKey = useRef("");
  const discoverPlacesRef = useRef(null);
  const searchSequence = useRef(0);
  const countryBrowseActive = Boolean(selectedDestination && isCountryOnly(selectedDestination));
  const browseCountry = selectedDestination?.country || selectedDestination?.name || "";
  const browsePlaces = countryCityTowns[browseCountry] || countryCityTowns[Object.keys(countryCityTowns).find((name) => name.toLowerCase() === browseCountry.toLowerCase())] || [];

  useEffect(() => {
    const initial = searchParams.get("destination");
    const initialCategory = searchParams.get("category");
    // URL search params are external navigation state used by the copilot and destination cards.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (initial) { setDestination(initial); setSelectedDestination(null); }
    if (initialCategory) setPlaceFilter(initialCategory);
  }, [searchParams]);

  useEffect(() => {
    const value = destination.trim();
    if (value.length < 2 || selectedDestination) { return undefined; }
    let active = true;
    const timer = window.setTimeout(async () => {
      setSuggestionLoading(true);
      try { const result = await searchLocations(value); if (active) setSuggestions(result.slice(0, 6)); }
      catch { if (active) setSuggestions([]); }
      finally { if (active) setSuggestionLoading(false); }
    }, 180);
    return () => { active = false; window.clearTimeout(timer); };
  }, [destination, selectedDestination]);

  async function discoverPlaces(nextFilter = placeFilter, destinationOverride = destination) {
    const requestId = ++searchSequence.current;
    setPlacesLoading(true); setPlacesError("");
    try {
      let location = selectedDestination;
      if (destinationOverride && location && !matchesLocationQuery(location, destinationOverride)) location = null;
      if (!location || location.latitude == null || location.longitude == null) {
        if (destinationOverride.trim().length < 2) throw new Error("Enter a city, town or landmark first.");
        location = await resolveLocation(destinationOverride.trim());
        if (requestId !== searchSequence.current) return;
        setSelectedDestination(location);
        setDestination(locationLabel(location));
      }
      if (isCountryOnly(location)) { setSelectedDestination(location); setPlaces([]); setPlacesError(""); setSuggestions([]); return; }
      setSuggestions([]);
      const baseOptions = { latitude: location.latitude, longitude: location.longitude, limit: 60, radiusKm: 25 };
      // Ask for the chosen category and a general nearby set at the same time. If
      // category tags are sparse in the public map data, the broader result still
      // gives the traveller useful options without another full timeout cycle.
      const requests = nextFilter === "HOTEL"
        ? ["HOTEL", "RESORT", "HOSTEL", "GUEST_HOUSE"].map((category) => getNearbyPlaces({ ...baseOptions, category }))
        : [getNearbyPlaces({ ...baseOptions, category: nextFilter || undefined })];
      if (nextFilter && nextFilter !== "HOTEL") requests.push(getNearbyPlaces(baseOptions));
      const settled = await Promise.allSettled(requests);
      if (requestId !== searchSequence.current) return;
      const validResults = settled.filter((item) => item.status === "fulfilled").map((item) => Array.isArray(item.value) ? item.value : []);
      if (!validResults.length) throw settled.find((item) => item.status === "rejected")?.reason || new Error("Place search is temporarily unavailable.");
      let result = nextFilter === "HOTEL"
        ? validResults.flat().filter((place, index, all) => all.findIndex((candidate) => (candidate.id || `${candidate.name}-${candidate.latitude}-${candidate.longitude}`) === (place.id || `${place.name}-${place.latitude}-${place.longitude}`)))
        : validResults[0] || [];
      if (!result.length && nextFilter && nextFilter !== "HOTEL" && validResults[1]?.length) {
        result = validResults[1];
        setPlacesError(`No ${nextFilter.replaceAll("_", " ").toLowerCase()} listings were mapped nearby, so we’re showing other mapped places instead.`);
      }
      const sortedResult = [...(Array.isArray(result) ? result : [])].sort((a, b) => {
        const ratingOf = (place) => {
          const raw = place.rating ?? place.reviewRating ?? place.userRating;
          const rating = Number(raw);
          return Number.isFinite(rating) && rating >= 0 && rating <= 5 ? rating : -1;
        };
        const ratingDifference = ratingOf(b) - ratingOf(a);
        if (ratingDifference !== 0) return ratingDifference;
        const aNamed = a.name ? 1 : 0, bNamed = b.name ? 1 : 0;
        return (bNamed - aNamed) || ((Number(a.distanceKm) || 99999) - (Number(b.distanceKm) || 99999));
      });
      setPlaces(sortedResult);
      if (!sortedResult.length) setPlacesError("The public map source returned no nearby listings for this area. You can still search the destination directly on Google Maps below.");
    } catch (error) {
      if (requestId !== searchSequence.current) return;
      setPlaces([]); setPlacesError(error?.message || "Place search is temporarily unavailable. Please try again.");
    } finally { if (requestId === searchSequence.current) setPlacesLoading(false); }
  }

  const chooseSuggestion = (item) => {
    searchSequence.current += 1;
    setPlacesLoading(false);
    setSelectedDestination(item);
    setDestination(locationLabel(item));
    setSuggestions([]); setPlacesError("");
  };

  useEffect(() => { discoverPlacesRef.current = discoverPlaces; });

  useEffect(() => {
    if (searchParams.get("autoSearch") !== "1") return;
    const key = `${searchParams.get("destination") || ""}|${searchParams.get("category") || ""}`;
    if (!key || autoSearchKey.current === key) return;
    autoSearchKey.current = key;
    if (searchParams.get("category")) {
      // The selected filter mirrors the explicit category requested by the copilot.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setPlaceFilter(searchParams.get("category"));
    }
    if (searchParams.get("destination")) discoverPlacesRef.current?.(searchParams.get("category") || "", searchParams.get("destination"));
  }, [searchParams]);

  return <main className="explore-page">
    <section className="explore-hero-v2">
      <div className="explore-hero-v2-copy"><span className="explore-eyebrow">ROAMORA / EXPLORE</span><h1>Find your kind<br/>of <em>somewhere.</em></h1><p>Discover cities, local food, places to stay and interesting stops. Search first; open verified map locations when you are ready.</p><a className="explore-main-cta" href="#place-search">Find places <ArrowRight size={17}/></a></div>
      <div className="explore-hero-v2-image"><img src="https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1400&q=82" alt="Mountain lake and scenic travel landscape" fetchPriority="high" onError={handleImageError}/><div className="explore-hero-image-caption"><MapPin size={15}/> Make room for the unexpected.</div></div>
    </section>

    <section className="explore-destinations-v2 section-shell"><div className="explore-v2-heading"><div><span className="explore-eyebrow">Destination ideas</span><h2>Start with a place.</h2></div><p>Photos are inspiration; use search below for location-based results.</p></div><div className="explore-destination-grid">{destinations.map((item)=><button type="button" className="explore-destination-card" key={item.name} onClick={()=>{const query = `${item.name}, ${item.country}`;setDestination(query);setSelectedDestination(null);setPlaces([]);setPlacesError("");document.getElementById("place-search")?.scrollIntoView({behavior:"auto",block:"start"});discoverPlaces(placeFilter, query);}}><img src={item.image} alt={item.alt} loading="lazy" onError={handleImageError}/><span className="explore-destination-overlay"/><span className="explore-destination-label"><small>{item.country}</small><strong>{item.name}</strong></span><span className="explore-destination-arrow"><ArrowRight size={16}/></span></button>)}</div><p className="explore-photo-credit">Destination photography is for inspiration; place results and map links are based on live search data. Images via <a href="https://unsplash.com" target="_blank" rel="noopener noreferrer">Unsplash</a>.</p></section>

    <section id="place-search" className="explore-search-section-v2 section-shell"><div className="explore-v2-heading"><div><span className="explore-eyebrow">Search real places</span><h2>What are you looking for?</h2></div><p>Type a city, town or landmark and select a geographic result to search around its coordinates.</p></div>
      <div className="explore-search-panel-v2"><form className="explore-search-form-v2" onSubmit={(event)=>{event.preventDefault();discoverPlaces();}}><div className="explore-search-input-wrap"><Search size={19}/><input aria-label="Destination" value={destination} onChange={(event)=>{searchSequence.current += 1;setPlacesLoading(false);setDestination(event.target.value);setSelectedDestination(null);setSuggestions([]);setPlaces([]);setPlacesError("");}} placeholder="Try Lisbon, Kyoto, Nellore or Central Park…" autoComplete="off"/><button type="submit" aria-label="Search places" disabled={placesLoading || destination.trim().length<2}><ArrowRight size={18}/></button></div>{suggestionLoading&&<p className="explore-search-hint">Searching locations…</p>}{suggestions.length>0&&!selectedDestination&&<div className="explore-suggestions-v2">{suggestions.map((item)=><button type="button" key={`${item.name}-${item.latitude}-${item.longitude}`} onClick={()=>chooseSuggestion(item)}><MapPin size={16}/><span><strong>{item.name}</strong><small>{[item.region,item.district,item.country,item.latitude?.toFixed?.(3),item.longitude?.toFixed?.(3)].filter(Boolean).join(" · ")}</small></span></button>)}</div>}
      <div className="explore-filters-v2">{filters.map(({value,label,icon:Icon})=><button type="button" key={label} className={placeFilter===value?"active":""} onClick={()=>{setPlaceFilter(value);if(selectedDestination)discoverPlaces(value);}}><Icon size={16}/>{label}</button>)}</div></form></div>
      {countryBrowseActive && <div className="explore-country-browser-v2">
        <div className="explore-country-browser-head"><div><span className="explore-eyebrow">DESTINATION DIRECTORY</span><h3>Explore {browseCountry} by city or town</h3><p>Choose a destination below or enter any city, town, or village name. Roamora will search mapped places around that location.</p></div><span>{browsePlaces.length} suggested destinations</span></div>
        <form className="explore-country-town-form" onSubmit={(event) => { event.preventDefault(); const place = countryPlaceInput.trim(); if (!place) return; const next = `${place}, ${browseCountry}`; setDestination(next); setSelectedDestination(null); setCountryPlaceInput(""); setPlaces([]); setPlacesError(""); discoverPlaces(placeFilter, next); }}><input value={countryPlaceInput} onChange={(event) => setCountryPlaceInput(event.target.value)} placeholder={`Enter a city or town in ${browseCountry}`} aria-label={`City or town in ${browseCountry}`} /><button type="submit" disabled={!countryPlaceInput.trim() || placesLoading}><Search size={15}/> Search destination</button></form>
        {browsePlaces.length > 0 && <div className="explore-country-town-grid">{browsePlaces.map((place) => <button key={place} type="button" onClick={() => { const next = `${place}, ${browseCountry}`; setDestination(next); setSelectedDestination(null); setPlaces([]); setPlacesError(""); discoverPlaces(placeFilter, next); }}>{place}<ArrowRight size={14}/></button>)}</div>}
      </div>}
      {placesError&&<div className="explore-feedback-v2" role="status">{placesError}{!places.length && selectedDestination && <div className="explore-map-fallback"><a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${placeFilter ? placeFilter.replaceAll("_", " ").toLowerCase() : "places"} in ${locationLabel(selectedDestination)}`)}`} target="_blank" rel="noopener noreferrer">Search current map listings <ExternalLink size={14}/></a></div>}</div>}
      {places.length>0&&<div className="explore-results-head-v2"><h3>{places.length} mapped place{places.length===1?"":"s"} found</h3><span>Location data from connected map sources</span></div>}
      {places.length>0&&<div className="explore-results-grid-v2">{places.map((place,index)=>{const category=String(place.category||place.type||"").toUpperCase();const photo=fallbackPhoto[category]||fallbackPhoto.ATTRACTION;return <article className="explore-result-card-v2" key={place.id||`${place.name}-${index}`}><div className="explore-result-photo"><img src={placePhotoUrl(place, photo)} alt={`${place.name || category.replaceAll("_"," ").toLowerCase() || "Travel place"} photo`} loading="lazy" onError={(event)=>{event.currentTarget.onerror=null;event.currentTarget.src=photo;}}/><span>{place.imageUrl ? "Place photo" : "Representative photo"}</span></div><div className="explore-result-content"><span className="explore-result-category">{category.replaceAll("_"," ")||"PLACE"}</span><h3>{place.name||"Unnamed mapped place"}</h3><p>{place.address||"Address not provided by the map source."}</p>{place.distanceKm!=null&&<small>{Number(place.distanceKm).toFixed(1)} km from your search point</small>}<small className="explore-result-rating">{Number.isFinite(Number(place.rating ?? place.reviewRating ?? place.userRating)) && String(place.rating ?? place.reviewRating ?? place.userRating ?? "").trim() !== "" && Number(place.rating ?? place.reviewRating ?? place.userRating) >= 0 && Number(place.rating ?? place.reviewRating ?? place.userRating) <= 5 ? `Customer rating ${Number(place.rating ?? place.reviewRating ?? place.userRating).toFixed(1)} / 5` : place.starRating ? `Listed class ${place.starRating}★ · customer review rating not supplied` : "Customer review rating not supplied by map source"}</small><small className="explore-result-price">{place.priceInfo ? `Listed price / fee: ${place.priceInfo}` : "Price not listed by map source"}</small><div className="explore-result-actions"><a href={mapUrl(place)} target="_blank" rel="noopener noreferrer">Open map <ExternalLink size={14}/></a>{safeExternalUrl(place.website) ? <a href={safeExternalUrl(place.website)} target="_blank" rel="noopener noreferrer">Official website <ExternalLink size={14}/></a> : <a href={`https://www.google.com/search?q=${encodeURIComponent(`${place.name} ${locationLabel(selectedDestination)} official website`)}`} target="_blank" rel="noopener noreferrer">Find website <ExternalLink size={14}/></a>}<a href={`https://www.google.com/search?q=${encodeURIComponent(`${place.name} ${locationLabel(selectedDestination)} price`)}`} target="_blank" rel="noopener noreferrer">Check prices <ExternalLink size={14}/></a></div></div></article>;})}</div>}
      {!places.length&&!placesLoading&&!placesError&&<div className="explore-search-empty"><LocateFixed size={20}/><span>Select a location and search to see nearby results.</span></div>}
    </section>

    <section className="explore-food-section-v2"><div className="section-shell"><div className="explore-v2-heading"><div><span className="explore-eyebrow">Eat your way around</span><h2>Good trips include <em>good food.</em></h2></div><p>Food photography is illustrative. Nearby restaurant and café listings come from your search results.</p></div><div className="explore-food-grid-v2">{foodPhotos.map((item)=><button type="button" className="explore-food-card-v2" key={item.title} onClick={()=>{setPlaceFilter(item.category);document.getElementById("place-search")?.scrollIntoView({behavior:"auto",block:"start"});if(selectedDestination)discoverPlaces(item.category);}}><img src={item.image} alt={item.alt} loading="lazy" onError={handleImageError}/><span className="explore-food-copy"><strong>{item.title}</strong><small>{item.text}</small><span>Explore food <ArrowRight size={14}/></span></span></button>)}</div></div></section>
    <section className="explore-bottom-link-v2 section-shell"><div><h2>Found a destination?</h2><p>Plan how to get there, then use Roamora to explore stays and stops along the way.</p></div><div><Link to="/planner">Plan a journey <ArrowRight size={16}/></Link><Link to="/stays">Find a stay <ArrowRight size={16}/></Link><Link to="/transport">Transport status <ArrowRight size={16}/></Link></div></section>
  </main>;
}
