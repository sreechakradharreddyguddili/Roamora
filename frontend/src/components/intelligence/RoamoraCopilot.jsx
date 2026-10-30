import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Bot,
  ChevronDown,
  Compass,
  ExternalLink,
  MapPinned,
  MessageCircle,
  Navigation,
  Send,
  Sparkles,
  X,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

import { askRoamoraCrewPlan, askRoamoraIntelligence } from "../../api/intelligenceApi";
import { storage } from "../../utils/storage";

import "./RoamoraCopilot.css";

function number(value, decimals = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return null;
  return Number(value).toFixed(decimals);
}

function duration(minutes) {
  if (minutes === null || minutes === undefined || Number.isNaN(Number(minutes))) return null;
  const total = Math.max(0, Math.round(Number(minutes)));
  const hours = Math.floor(total / 60);
  const mins = total % 60;
  if (!hours) return `${mins} min`;
  return mins ? `${hours} hr ${mins} min` : `${hours} hr`;
}

function weatherLabel(code) {
  if (code === null || code === undefined || Number.isNaN(Number(code))) return "current conditions";
  const value = Number(code);
  if (value === 0) return "clear skies";
  if (value <= 3) return "partly cloudy conditions";
  if (value <= 48) return "fog or reduced visibility";
  if (value <= 67) return "rain conditions";
  if (value <= 77) return "snow conditions";
  if (value <= 82) return "showers";
  if (value <= 86) return "snow showers";
  return "storm conditions";
}

function selectedRoute(analysis) {
  return analysis?.alternativeRoutes?.find((item) => item.recommended)
    || analysis?.alternatives?.find((item) => item.recommended)
    || analysis?.alternativeRoutes?.[0]
    || analysis?.alternatives?.[0]
    || null;
}

function buildReply(question, analysis, locations) {
  const text = question.trim().toLowerCase();

  if (!text) return "Ask me anything about Roamora or your journey.";

  if (!analysis) {
    if (/\b(hello|hi|hey|good morning|good evening|who are you)\b/.test(text)) {
      return "Hi! I’m Roamora, your travel copilot. I can help you plan a journey, discover restaurants and attractions, find accommodation, understand transport schedules, or show you where to go in the app. Tell me the place or task and I’ll guide you there.";
    }
    if (/\b(transport|bus|train|flight|arrival|departure|live tracking|vehicle|delay)\b/.test(text)) {
      return "Open Transport to choose an available operator or city feed. Where an operator publishes real-time data, Roamora can show vehicle positions, trip updates, scheduled and estimated times, delays and alerts. If only timetable data is available, I’ll say so rather than inventing a live location.";
    }
    if (/\b(india|state|city|town|district)\b/.test(text) && /\b(stay|hotel|explore|search|find|location)\b/.test(text)) {
      return "For India, search by city rather than the whole country so results are geographically useful. For example, try Nellore, Visakhapatnam, Hyderabad, Bengaluru or Jaipur. In Stays, select a mapped city result first; in Explore, do the same before searching for food and attractions.";
    }
    if (/\b(budget|cheap|affordable|cost|expense|money)\b/.test(text)) {
      return "I can help you plan around a budget, but I won’t invent live fares or hotel prices. Tell me your destination, dates, number of travellers and approximate budget, then compare transport options, nearby stays and food in Roamora. Provider prices must be confirmed before booking.";
    }
    if (/\b(pack|packing|checklist|what should i bring)\b/.test(text)) {
      return "A useful travel checklist: ID/passport and required visas, tickets and booking confirmations, payment options, medicines you normally need, chargers and power adapter, weather-appropriate layers, water, and copies of key documents. Tell me the destination, season and trip length and I’ll tailor it.";
    }
    if (/\b(safety|safe|emergency|emergencies)\b/.test(text)) {
      return "I can help you prepare, but I can’t guarantee a place or route is safe. Check current official travel advisories, local emergency numbers, weather alerts and operator updates. In Roamora, plan your route and review available weather and risk context; in Explore, look for mapped hospitals and pharmacies.";
    }
    if (/\b(restaurant|food|eat|cafe|coffee|attraction|museum|things to do)\b/.test(text)) {
      return "I can help you find food and things to do. Tell me the city or landmark, then open Explore, choose Restaurants, Cafés or Attractions, and search nearby. Results depend on map coverage; open the map link to verify the exact place before heading there.";
    }
    if (/\b(booking|book|reserve|reservation|buy tickets)\b/.test(text)) {
      return "Roamora helps you discover and plan, but it doesn’t confirm a reservation or purchase unless a connected booking provider explicitly supports that action. I can guide you to the right place, help compare options and open the provider’s map or website where available.";
    }
    if (/\b(itinerary|day trip|two days|3 days|three days|weekend|multi.day|schedule my trip)\b/.test(text)) {
      return "I can build a practical itinerary. Share your destination, trip length, dates and pace (relaxed or packed). I’ll organize nearby sights by area, leave time for meals and transit, and flag details that need live confirmation. In the meantime, use Explore to collect mapped places and Planner to compare journeys.";
    }
    if (/\b(visa|passport|entry requirements|customs|border)\b/.test(text)) {
      return "Entry rules depend on your citizenship, destination and travel dates. Check the destination government’s official immigration site or your embassy before booking; I won’t guess visa eligibility. Tell me your passport country and destination and I can help identify the official questions to verify.";
    }
    if (/\b(best time|when should i go|season|climate|weather in)\b/.test(text)) {
      return "The best time depends on the destination, weather tolerance, crowds and budget. Tell me the city and month you’re considering and I’ll help compare seasonal trade-offs. For current conditions, use a live weather source close to departure rather than relying on seasonal averages.";
    }
    if (/\b(accessib|wheelchair|mobility|child|family|kids|solo travel|travelling alone)\b/.test(text)) {
      return "I can tailor recommendations for accessibility, families or solo travel. Tell me the destination and the main constraint (step-free access, short walking distances, child-friendly stops, late-night arrival or another need). Place listings may not include complete accessibility details, so confirm with the venue before you go.";
    }
    if (/\b(time zone|local time|jet lag|time difference)\b/.test(text)) {
      return "I can help compare time zones and plan around jet lag. Share your departure city, destination and travel date; daylight-saving changes can affect the exact difference, so confirm the local time for the date of travel.";
    }
    if (/\b(hello|hi|hey|good morning|good evening|who are you)\b/.test(text)) {
      return "Hi! I’m Roamora, your travel copilot. I can help you plan a journey, discover restaurants and attractions, find accommodation, understand transport schedules, or show you where to go in the app. Tell me the place or task and I’ll guide you there.";
    }
    if (text.includes("how") && text.includes("work")) {
      return "Roamora connects a route with weather and risk context, then gives you alternatives so you can make the travel decision with more information instead of just seeing a line on a map.";
    }

    if (text.includes("weather")) {
      return "On the Planner, enter your origin and destination. Roamora will bring weather signals into the journey analysis so you can see conditions before you leave.";
    }

    if (text.includes("stay") || text.includes("hotel") || text.includes("accommodation")) {
      return "Open Stays to discover nearby accommodation around a place. You can also use Explore to find useful places such as restaurants, attractions, fuel and EV charging.";
    }

    if (text.includes("explore") || text.includes("nearby") || text.includes("places")) {
      return "Explore is Roamora's place-discovery layer. Search a location, choose a category, and open a place directly in maps when you find something useful.";
    }

    if (text.includes("risk") || text.includes("safe")) {
      return "Risk is contextual rather than a guarantee of safety. Roamora combines the signals available for the planned route and explains the result so you can decide what matters to you.";
    }

    return "I’m Roamora Intelligence. I can explain the planner, help you understand route risk and weather, or guide you to Explore and Stays. Once you calculate a route, I can answer questions about that exact journey.";
  }

  const route = selectedRoute(analysis);
  const risk = route?.riskScore ?? analysis.riskScore;
  const riskLevel = String(route?.riskLevel ?? analysis.riskLevel ?? "unknown").replaceAll("_", " ").toLowerCase();
  const distance = route?.distanceKm ?? analysis.distanceKm;
  const travelTime = route?.durationMinutes ?? analysis.durationMinutes;
  const originTemperature = analysis.originTemperature;
  const destinationTemperature = analysis.destinationTemperature;

  if (text.includes("risk") || text.includes("safe") || text.includes("danger")) {
    return `Roamora currently describes the selected route as ${riskLevel}${risk !== null && risk !== undefined ? ` (${risk}/100)` : ""}. ${route?.recommendation || analysis.recommendation || "Review the route conditions before departure."}`;
  }

  if (text.includes("weather") || text.includes("rain") || text.includes("temperature") || text.includes("hot") || text.includes("cold")) {
    const originWeather = weatherLabel(analysis.originWeatherCode);
    const destinationWeather = weatherLabel(analysis.destinationWeatherCode);
    const originTemp = number(originTemperature, 0);
    const destinationTemp = number(destinationTemperature, 0);
    return `${locations?.origin || "Origin"}: ${originWeather}${originTemp !== null ? `, ${originTemp}°C` : ""}. ${locations?.destination || "Destination"}: ${destinationWeather}${destinationTemp !== null ? `, ${destinationTemp}°C` : ""}. Open the Conditions section for the detailed signals Roamora received.`;
  }

  if (text.includes("time") || text.includes("long") || text.includes("duration")) {
    return `The selected route is about ${number(distance, 1) || "—"} km and takes roughly ${duration(travelTime) || "—"}. You can compare alternatives if speed is not your only priority.`;
  }

  if (text.includes("alternative") || text.includes("other route") || text.includes("compare")) {
    const count = analysis.alternativeRoutes?.length || analysis.alternatives?.length || 0;
    return `Roamora found ${count} route option${count === 1 ? "" : "s"}. Select the alternatives in the Decision section to compare their distance, duration and risk context.`;
  }

  if (text.includes("pack") || text.includes("prepare") || text.includes("before") || text.includes("carry")) {
    return `Before leaving, check the departure time, conditions and risk context. For changing conditions, treat this as planning guidance rather than a guarantee and reassess close to departure.`;
  }

  if (text.includes("why") || text.includes("recommend")) {
    return `Roamora's recommendation is based on the route signals available to the planner. ${analysis.recommendation || route?.recommendation || "Compare the route cards to see the underlying trade-offs."}`;
  }

  return `For ${locations?.origin || "your origin"} → ${locations?.destination || "your destination"}, Roamora has analyzed the available route, weather and risk signals. The selected route is about ${number(distance, 1) || "—"} km and ${duration(travelTime) || "—"}. Ask me about risk, weather, time, alternatives or preparation.`;
}

const DEFAULT_SUGGESTIONS = [
  "Plan a trip to Rome",
  "Find restaurants near me",
  "Find stays in India",
  "How do live transport times work?",
];

const AI_ENABLED = import.meta.env.VITE_ROAMORA_AI_ENABLED === "true";

const JOURNEY_SUGGESTIONS = [
  "Why is this route recommended?",
  "What is the weather?",
  "How long will it take?",
  "What should I prepare?",
];

function resolveCopilotAction(question) {
  const text = question.trim();
  const lower = text.toLowerCase();
  const routeMatch = text.match(/\bfrom\s+(.+?)\s+to\s+(.+?)(?:[?.!,]|$)/i);
  const destinationMatch = text.match(/\b(?:in|around|near|for|to)\s+([\p{L}][\p{L}\p{M} ,.'-]{1,60}?)(?=\s+(?:and|but|then|please|show|find|with|what|where)\b|[?.!,]|$)/iu);
  const cleanPlace = (value) => value?.trim().replace(/\b(please|thanks|thank you|for me)$/i, "").replace(/[?.!,]+$/, "").trim();
  const rawDestination = cleanPlace(destinationMatch?.[1]);
  const destination = rawDestination && !/^(my journey|my location|here|there|the destination|my trip|me)$/i.test(rawDestination) ? rawDestination : undefined;
  const origin = cleanPlace(routeMatch?.[1]);
  const routeDestination = cleanPlace(routeMatch?.[2]);

  if (/\b(sign ?up|create (?:an )?account|register)\b/.test(lower)) return { path: "/signup", message: "I’ve opened account creation. After registration, you’ll be taken to sign in." };
  if (/\b(sign ?in|log ?in)\b/.test(lower)) return { path: "/login", message: "I’ve opened the sign-in page." };
  if (/\b(log ?out|sign ?out)\b/.test(lower)) return { path: "/profile", message: "Open your profile to sign out securely." };
  if (/\b(profile|my account|account settings)\b/.test(lower)) return { path: "/profile", message: "I’ve opened your profile. Sign in if authentication is required." };
  if (/\b(history|past journeys|previous trips|saved routes)\b/.test(lower)) return { path: "/history", message: "I’ve opened your journey history." };
  if (/\b(transport|live bus|live train|arrival time|departure time|vehicle tracking|delays|transit)\b/.test(lower)) return { path: "/transport", message: "I’ve opened Transport. Choose an available operator feed to view published vehicle positions, schedules, arrival estimates and service alerts. Live coverage depends on the operator." };
  if (/\b(about roamora|what is roamora|about this app)\b/.test(lower)) return { path: "/about", message: "I’ve opened About Roamora, where you can learn how the travel assistant connects planning, places, stays and transport data." };
  if (/\b(feature|features|what can you do|capabilities)\b/.test(lower)) return { path: "/features", message: "I’ve opened Roamora’s feature guide. You can plan routes, review weather and route context, discover places, find stays and check supported transport feeds." };
  if (/\b(dashboard|overview|my trips)\b/.test(lower)) return { path: "/dashboard", message: "I’ve opened your dashboard. Sign in if needed to view your saved journey overview." };
  if (routeMatch && /\b(plan|planner|route|journey|trip|directions|go)\b/.test(lower)) {
    return { path: `/planner?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(routeDestination)}`, message: `I’ve filled in ${origin} → ${routeDestination} in the planner. Choose the matching geographic suggestions and submit to calculate the route.` };
  }
  if (/\b(hotel|hotels|stay|stays|hostel|resort|accommodation|guest house)\b/.test(lower)) {
    const countryOnly = destination && /^(india|canada|france|italy|united states|usa|united kingdom|uk|australia|japan|germany|spain)$/i.test(destination);
    return { path: `/stays${destination ? `?destination=${encodeURIComponent(destination)}${countryOnly ? "" : "&autoSearch=1"}` : ""}`, message: countryOnly ? `I’ve opened Stays with ${destination} selected. Choose a state/region and city first so Roamora can search the correct area instead of a country-wide centre point.` : destination ? `I’ve started a mapped accommodation search around ${destination}. Review the location and results in Stays.` : "I’ve opened Stays. Enter a destination to find mapped accommodation." };
  }
  if (/\b(food|restaurant|restaurants|eat|eating|cafe|cafes|café|cafés|coffee)\b/.test(lower)) return { path: `/explore${destination ? `?destination=${encodeURIComponent(destination)}&category=${/\b(cafe|cafes|café|cafés|coffee)\b/.test(lower) ? "CAFE" : "RESTAURANT"}&autoSearch=1` : ""}`, message: destination ? `I’ve started a nearby food search for ${destination}. Review the mapped results in Explore.` : "I’ve opened Explore. Choose a destination and the Food or Cafés filter to search nearby." };
  if (/\b(weather|forecast|temperature)\b/.test(lower) && destination) return { path: `/explore?destination=${encodeURIComponent(destination)}`, message: `I’ve opened Explore for ${destination}. For weather connected to a trip, open the Planner and analyze a route; standalone destination forecasts are not yet a separate screen.` };
  if (/\b(explore|nearby|attractions|things to do|places to visit|show me|find places|open map|show map)\b/.test(lower)) return { path: `/explore${destination ? `?destination=${encodeURIComponent(destination)}` : ""}`, message: destination ? `I’ve opened Explore for ${destination}. Select the matching geographic result and search nearby.` : "I’ve opened Explore so you can search cities, food, stays and attractions." };
  if (/\b(plan|planner|route|journey|directions|trip)\b/.test(lower)) return { path: `/planner${destination ? `?destination=${encodeURIComponent(destination)}` : ""}`, message: destination ? `I’ve opened the planner with ${destination} as your destination. Choose the mapped suggestion and add your starting point to calculate the route.` : "I’ve opened the journey planner. Enter your start and destination to calculate a supported route." };
  return null;
}

export default function RoamoraCopilot() {
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [journeyContext, setJourneyContext] = useState(null);
  const [thinking, setThinking] = useState(false);
  const inputRef = useRef(null);
  const messagesEndRef = useRef(null);
  const askRef = useRef(null);

  useEffect(() => {
    const handleAnalysis = (event) => setJourneyContext(event.detail || null);
    window.addEventListener("roamora:analysis", handleAnalysis);
    return () => window.removeEventListener("roamora:analysis", handleAnalysis);
  }, []);

  useEffect(() => {
    const handleKey = (event) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, []);

  useEffect(() => {
    if (open) {
      window.setTimeout(() => inputRef.current?.focus(), 80);
    }
  }, [open]);

  const analysis = journeyContext?.analysis || null;
  const locations = journeyContext?.locations || null;

  const suggestions = useMemo(
    () => (analysis ? JOURNEY_SUGGESTIONS : DEFAULT_SUGGESTIONS),
    [analysis]
  );

  const ask = useCallback(async (value) => {
    const clean = value.trim();
    if (!clean || thinking) return;

    setQuestion("");
    setOpen(true);
    setMessages((current) => [
      ...current.slice(-7),
      { role: "user", text: clean },
    ]);

    const action = resolveCopilotAction(clean);
    if (action) {
      navigate(action.path);
      setMessages((current) => [...current, { role: "assistant", text: action.message }]);
      return;
    }

    const localReply = buildReply(clean, analysis, locations);
    const session = storage.getSession();
    const journeyContext = JSON.stringify({
      origin: locations?.origin || null,
      destination: locations?.destination || null,
      transportMode: locations?.transportMode || null,
      departureDate: locations?.departureDate || null,
      departureTime: locations?.departureTime || null,
      analysis: analysis || null,
      recentConversation: messages.slice(-8).map(({ role, text }) => ({ role, text })),
      currentPage: location.pathname,
    });

    if (!AI_ENABLED || !session?.token) {
      setMessages((current) => [
        ...current,
        { role: "assistant", text: localReply },
      ]);
      return;
    }

    setThinking(true);

    try {
      const aiReply = await askRoamoraIntelligence({
        question: clean,
        journeyContext,
      });

      setMessages((current) => [
        ...current,
        { role: "assistant", text: aiReply || localReply },
      ]);
    } catch (error) {
      console.warn("Roamora Intelligence AI unavailable; using local context:", error);
      setMessages((current) => [
        ...current,
        { role: "assistant", text: localReply },
      ]);
    } finally {
      setThinking(false);
    }
  }, [analysis, locations, navigate, thinking, messages, location.pathname]);

  const runCrewPlan = useCallback(async () => {
    if (thinking) return;
    const latestUserPrompt = [...messages].reverse().find((message) => message.role === "user")?.text;
    const clean = (latestUserPrompt || question).trim();
    const session = storage.getSession();
    if (!clean) {
      setMessages((current) => [...current, { role: "assistant", text: "Ask me for a destination or travel plan first, then I can send it to the multi-agent planning team." }]);
      return;
    }
    if (!AI_ENABLED || !session?.token) {
      setMessages((current) => [...current, { role: "assistant", text: "Sign in and enable the Roamora AI service to use the CrewAI planning team." }]);
      return;
    }
    const context = JSON.stringify({
      origin: locations?.origin || null,
      destination: locations?.destination || null,
      transportMode: locations?.transportMode || null,
      departureDate: locations?.departureDate || null,
      departureTime: locations?.departureTime || null,
      analysis: analysis || null,
      recentConversation: messages.slice(-8).map(({ role, text }) => ({ role, text })),
      currentPage: location.pathname,
    });
    setThinking(true);
    try {
      const answer = await askRoamoraCrewPlan({ question: clean, journeyContext: context });
      setMessages((current) => [...current, { role: "assistant", text: `CrewAI travel team\n\n${answer}` }]);
    } catch (error) {
      setMessages((current) => [...current, { role: "assistant", text: error.message || "The CrewAI planning team is unavailable. Check that the Roamora agent service is running." }]);
    } finally {
      setThinking(false);
    }
  }, [analysis, locations, location.pathname, messages, question, thinking]);

  useEffect(() => { askRef.current = ask; }, [ask]);

  useEffect(() => {
    if (open) messagesEndRef.current?.scrollIntoView({ behavior: "auto", block: "end" });
  }, [messages, thinking, open]);

  useEffect(() => {
    const handleCopilotAsk = (event) => {
      const prompt = typeof event.detail === "string" ? event.detail : "";
      if (!prompt) return;
      setOpen(true);
      void askRef.current?.(prompt);
    };
    window.addEventListener("roamora:copilot-ask", handleCopilotAsk);
    return () => window.removeEventListener("roamora:copilot-ask", handleCopilotAsk);
  }, []);

  const openPage = (path) => {
    setOpen(false);
    navigate(path);
  };

  const showContextAction = location.pathname !== "/planner" && analysis;

  return (
    <div className={`roamora-copilot ${open ? "is-open" : ""}`}>
      {open && (
        <section className="copilot-panel" aria-label="Roamora Intelligence">
          <header className="copilot-header">
            <div className="copilot-avatar"><Bot size={18} /></div>
            <div>
              <strong>Roamora Intelligence</strong>
              <span>{analysis ? "Your journey-aware copilot" : "Your travel planning copilot"}</span>
            </div>
            <button type="button" onClick={() => setOpen(false)} aria-label="Close Roamora Intelligence">
              <X size={17} />
            </button>
          </header>

          <div className="copilot-status">
            <Sparkles size={14} />
            <span>{analysis ? "Connected to your current journey" : "Ready whenever you need me"}</span>
          </div>

          <div className="copilot-messages" aria-live="polite">
            {messages.length === 0 ? (
              <div className="copilot-welcome">
                <div className="copilot-welcome-icon"><Compass size={21} /></div>
                <strong>{analysis ? "Let’s make sense of this journey." : "Tell me what you need."}</strong>
                <p>
                  {analysis
                    ? "Ask about weather, risk, route trade-offs or what to prepare."
                    : "I can guide you around Roamora and explain how each travel feature works."}
                </p>
              </div>
            ) : (
              <>
              {messages.map((message, index) => (
                <div key={`${message.role}-${index}`} className={`copilot-message ${message.role}`}>
                  {message.text}
                </div>
              ))}
              {thinking && (
                <div className="copilot-message assistant copilot-thinking">
                  <span />
                  <span />
                  <span />
                </div>
              )}
              </>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="copilot-suggestions">
            {suggestions.map((suggestion) => (
              <button type="button" key={suggestion} onClick={() => ask(suggestion)} disabled={thinking}>
                {suggestion}
              </button>
            ))}
          </div>

          <div className="copilot-actions">
            <button type="button" onClick={() => openPage("/planner")}>
              <Navigation size={14} />
              Plan a journey
            </button>
            <button type="button" onClick={() => openPage("/explore")}>
              <MapPinned size={14} />
              Explore places
            </button>
            <button type="button" onClick={() => openPage("/stays")}>
              <ExternalLink size={14} />
              Find stays
            </button>
            <button type="button" onClick={() => openPage("/transport")}>
              <Navigation size={14} />
              Transport live data
            </button>
            <button type="button" onClick={runCrewPlan} disabled={thinking}>
              <Sparkles size={14} />
              Plan with AI team
            </button>
            {showContextAction && (
              <button type="button" onClick={() => openPage("/planner")}>
                <MessageCircle size={14} />
                Open journey
              </button>
            )}
          </div>

          <form
            className="copilot-input"
            onSubmit={(event) => {
              event.preventDefault();
              ask(question);
            }}
          >
            <input
              ref={inputRef}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              placeholder={thinking ? "Roamora is thinking…" : analysis ? "Ask about this journey..." : "Ask Roamora anything..."}
              aria-label="Ask Roamora Intelligence"
              disabled={thinking}
            />
            <button type="submit" aria-label="Send question" disabled={!question.trim() || thinking}>
              <Send size={16} />
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        className="copilot-trigger"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label="Open Roamora Intelligence"
      >
        <Bot size={18} />
        <span>Ask Roamora</span>
        <ChevronDown size={14} className={open ? "rotated" : ""} />
      </button>
    </div>
  );
}
