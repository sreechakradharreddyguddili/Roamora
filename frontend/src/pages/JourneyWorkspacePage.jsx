import { useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Accessibility,
  AlertTriangle,
  ArrowRight,
  CalendarDays,
  Check,
  Clock3,
  CloudSun,
  Copy,
  DollarSign,
  Download,
  Heart,
  MapPin,
  MessageCircle,
  Plus,
  ShieldCheck,
  Sparkles,
  Trash2,
  Users,
  Utensils,
} from "lucide-react";
import "./JourneyWorkspacePage.css";

function getRoute(analysis) {
  return analysis?.alternativeRoutes?.find((r) => r.recommended)
    || analysis?.alternatives?.find((r) => r.recommended)
    || analysis?.alternativeRoutes?.[0]
    || analysis?.alternatives?.[0]
    || null;
}

function formatNumber(value, digits = 1) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(digits) : "—";
}

function WorkspaceCard({ icon: Icon, eyebrow, title, children }) {
  return (
    <section className="workspace-card">
      <div className="workspace-card-heading">
        <span className="workspace-card-icon"><Icon size={17} /></span>
        <div><span>{eyebrow}</span><h2>{title}</h2></div>
      </div>
      {children}
    </section>
  );
}

export default function JourneyWorkspacePage() {
  const { state } = useLocation();
  const [items, setItems] = useState([
    { id: 1, label: "Check departure conditions", done: false },
    { id: 2, label: "Confirm accommodation", done: false },
    { id: 3, label: "Save an offline copy of the journey", done: false },
  ]);
  const [note, setNote] = useState("");
  const [notes, setNotes] = useState([]);
  const [copied, setCopied] = useState(false);

  const analysis = state?.analysis || null;
  const locations = state?.locations || null;
  const route = useMemo(() => getRoute(analysis), [analysis]);
  const routeName = `${locations?.origin || "Origin"} → ${locations?.destination || "Destination"}`;
  const distance = route?.distanceKm ?? route?.distance;
  const travelTime = route?.travelTimeMinutes ?? route?.durationMinutes ?? route?.duration;
  const risk = route?.riskLevel || route?.risk || analysis?.riskLevel || "Available in analysis";

  const toggleItem = (id) => setItems((current) => current.map((item) => item.id === id ? { ...item, done: !item.done } : item));
  const addNote = () => {
    if (!note.trim()) return;
    setNotes((current) => [{ id: Date.now(), text: note.trim() }, ...current]);
    setNote("");
  };

  const share = async () => {
    const text = `Roamora journey: ${routeName}${distance ? ` • ${formatNumber(distance)} km` : ""}`;
    try {
      if (navigator.share) await navigator.share({ title: "Roamora Journey", text });
      else if (navigator.clipboard) { await navigator.clipboard.writeText(text); setCopied(true); setTimeout(() => setCopied(false), 1600); }
    } catch { /* user cancelled */ }
  };

  if (!analysis || !locations) {
    return (
      <main className="workspace-page workspace-empty-page">
        <div className="workspace-empty">
          <Sparkles size={28} />
          <span>ROAMORA / JOURNEY WORKSPACE</span>
          <h1>Build a journey before you leave.</h1>
          <p>Plan a route first. Roamora will turn the result into a reusable workspace with decisions, preparation, notes and next actions.</p>
          <Link to="/planner" className="workspace-primary"><MapPin size={17} /> Open planner <ArrowRight size={16} /></Link>
        </div>
      </main>
    );
  }

  return (
    <main className="workspace-page">
      <section className="workspace-hero">
        <div>
          <span className="workspace-eyebrow"><span /> ROAMORA / JOURNEY WORKSPACE</span>
          <h1>{locations.origin}<br /><em>to {locations.destination}</em></h1>
          <p>One place for the journey itself — route context, preparation, decisions, places to discover and an AI copilot that understands this trip.</p>
          <div className="workspace-actions">
            <button type="button" className="workspace-primary" onClick={share}><Copy size={16} /> {copied ? "Copied" : "Share journey"}</button>
            <Link to="/planner" className="workspace-secondary">Edit route</Link>
          </div>
        </div>
        <div className="workspace-orbit" aria-hidden="true"><div /><div /><strong>ROAM</strong></div>
      </section>

      <section className="workspace-stats">
        <div><Clock3 size={18} /><span>TRAVEL TIME</span><strong>{travelTime ? `${formatNumber(travelTime, 0)} min` : "—"}</strong></div>
        <div><MapPin size={18} /><span>DISTANCE</span><strong>{distance ? `${formatNumber(distance)} km` : "—"}</strong></div>
        <div><ShieldCheck size={18} /><span>RISK CONTEXT</span><strong>{String(risk).replaceAll("_", " ")}</strong></div>
        <div><CalendarDays size={18} /><span>DEPARTURE</span><strong>{locations.departureDate || "Flexible"} {locations.departureTime || ""}</strong></div>
      </section>

      <div className="workspace-grid">
        <WorkspaceCard icon={Sparkles} eyebrow="01 / DECISION" title="Your journey brief">
          <div className="workspace-brief"><strong>What matters now</strong><p>Roamora has a route result, but the best choice depends on your priorities. Use the route comparison and current condition signals instead of treating one route as universally best.</p></div>
          <div className="workspace-decision-grid">
            <div><span>FASTEST</span><strong>{route?.travelTimeMinutes ? `${formatNumber(route.travelTimeMinutes, 0)} min` : "Compare routes"}</strong></div>
            <div><span>LOWER-RISK OPTION</span><strong>Inspect route cards</strong></div>
            <div><span>WEATHER</span><strong><CloudSun size={16} /> Connected</strong></div>
          </div>
        </WorkspaceCard>

        <WorkspaceCard icon={Check} eyebrow="02 / PREPARE" title="Before you leave">
          <div className="workspace-checklist">
            {items.map((item) => <button type="button" key={item.id} className={item.done ? "done" : ""} onClick={() => toggleItem(item.id)}><span>{item.done ? <Check size={14} /> : null}</span>{item.label}</button>)}
          </div>
          <button type="button" className="workspace-add" onClick={() => setItems((current) => [...current, { id: Date.now(), label: "New preparation item", done: false }])}><Plus size={15} /> Add preparation item</button>
        </WorkspaceCard>

        <WorkspaceCard icon={Heart} eyebrow="03 / TRIP FIT" title="Make it yours">
          <div className="workspace-preference-list">
            <div><Accessibility size={17} /><div><strong>Accessibility</strong><span>Prefer step-free, accessible options when provider data supports it.</span></div></div>
            <div><DollarSign size={17} /><div><strong>Budget-aware</strong><span>Keep cost visible when live prices are available instead of hiding trade-offs.</span></div></div>
            <div><Users size={17} /><div><strong>Group travel</strong><span>Save the workspace and share it with people travelling with you.</span></div></div>
          </div>
        </WorkspaceCard>

        <WorkspaceCard icon={MessageCircle} eyebrow="04 / ASK ROAMORA" title="Journey-aware AI">
          <p className="workspace-ai-copy">Ask questions about this exact journey. The assistant receives the route context and is instructed not to invent schedules, prices, weather or safety guarantees.</p>
          <div className="workspace-prompts">
            <button type="button">What should I know before leaving?</button>
            <button type="button">Give me alternatives and trade-offs.</button>
            <button type="button">What should I pack for this journey?</button>
          </div>
        </WorkspaceCard>

        <WorkspaceCard icon={MapPin} eyebrow="05 / DISCOVER" title="Complete the destination">
          <div className="workspace-discovery-grid">
            <Link to="/explore"><span>Places</span><small>Attractions, food & essentials</small><ArrowRight size={15} /></Link>
            <Link to="/stays"><span>Stays</span><small>Hotels and mapped accommodation</small><ArrowRight size={15} /></Link>
            <div><span>Transport</span><small>Only show options supported by live data</small><ArrowRight size={15} /></div>
            <div><span>Essentials</span><small>Hospitals, pharmacies, fuel & EV</small><ArrowRight size={15} /></div>
          </div>
        </WorkspaceCard>

        <WorkspaceCard icon={Utensils} eyebrow="06 / NOTES" title="Your trip memory">
          <div className="workspace-note-form"><input value={note} onChange={(e) => setNote(e.target.value)} onKeyDown={(e) => e.key === "Enter" && addNote()} placeholder="Add a note for this journey..." /><button type="button" onClick={addNote}><Plus size={16} /></button></div>
          <div className="workspace-notes">{notes.length ? notes.map((item) => <div key={item.id}><span>{item.text}</span><button type="button" onClick={() => setNotes((current) => current.filter((n) => n.id !== item.id))}><Trash2 size={14} /></button></div>) : <p>No notes yet. Add arrival instructions, food ideas, reservation details or anything you want to remember.</p>}</div>
        </WorkspaceCard>
      </div>

      <section className="workspace-trust">
        <AlertTriangle size={17} /> <div><strong>Roamora principle: availability beats invention.</strong><span>If a provider does not supply a flight, hotel price, transit schedule or live condition, Roamora will say that the data is unavailable rather than fabricate an answer.</span></div>
        <button type="button" onClick={() => window.print()}><Download size={15} /> Save / print</button>
      </section>
    </main>
  );
}
