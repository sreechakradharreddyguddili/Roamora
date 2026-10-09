import { useEffect, useMemo, useRef, useState } from "react";
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

import { askRoamoraIntelligence } from "../../api/intelligenceApi";
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
  "How does Roamora work?",
  "What can you analyze?",
  "Find places near my journey",
  "Where can I find stays?",
];

const AI_ENABLED = import.meta.env.VITE_ROAMORA_AI_ENABLED === "true";

const JOURNEY_SUGGESTIONS = [
  "Why is this route recommended?",
  "What is the weather?",
  "How long will it take?",
  "What should I prepare?",
];

export default function RoamoraCopilot() {
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [journeyContext, setJourneyContext] = useState(null);
  const [thinking, setThinking] = useState(false);
  const inputRef = useRef(null);

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

  const ask = async (value) => {
    const clean = value.trim();
    if (!clean || thinking) return;

    setQuestion("");
    setOpen(true);
    setMessages((current) => [
      ...current.slice(-7),
      { role: "user", text: clean },
    ]);

    const localReply = buildReply(clean, analysis, locations);
    const session = storage.getSession();
    const journeyContext = JSON.stringify({
      origin: locations?.origin || null,
      destination: locations?.destination || null,
      transportMode: locations?.transportMode || null,
      departureDate: locations?.departureDate || null,
      departureTime: locations?.departureTime || null,
      analysis: analysis || null,
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
  };

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
          </div>

          <div className="copilot-suggestions">
            {suggestions.map((suggestion) => (
              <button type="button" key={suggestion} onClick={() => ask(suggestion)}>
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
              placeholder={analysis ? "Ask about this journey..." : "Ask Roamora anything..."}
              aria-label="Ask Roamora Intelligence"
            />
            <button type="submit" aria-label="Send question" disabled={!question.trim()}>
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
        <span className="copilot-trigger-orbit" />
        <Bot size={18} />
        <span>Ask Roamora</span>
        <ChevronDown size={14} className={open ? "rotated" : ""} />
      </button>
    </div>
  );
}
