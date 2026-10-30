import { useMemo, useState } from "react";
import { AlertTriangle, ArrowRight, CheckCircle2, Clock3, Compass, Download, ShieldCheck, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import "./JourneyResiliencePage.css";

const SCENARIOS = [
  { id: "weather", label: "Weather changes", detail: "Rain, heat, fog or severe conditions", actions: ["Check a forecast close to departure and again before a long leg.", "Identify a sheltered stop or a safe place to pause.", "Avoid treating a forecast as a guarantee; conditions can change."] },
  { id: "closure", label: "Road or route disruption", detail: "Closure, congestion or an unexpected detour", actions: ["Save the primary route and identify a plausible alternate corridor.", "Keep enough battery and connectivity to re-check the route.", "Do not follow an alternate route unless it is confirmed navigable."] },
  { id: "service", label: "A service is unavailable", detail: "Fuel, food, lodging or charging stop unavailable", actions: ["Identify a second stop before leaving the current service area.", "Check opening hours and availability directly where possible.", "Keep essential supplies appropriate to the journey length."] },
  { id: "connection", label: "Network or phone battery loss", detail: "Limited connectivity during the journey", actions: ["Save route details and important addresses offline.", "Charge the phone and carry a suitable power bank if needed.", "Share the plan and expected arrival window with a trusted contact."] },
  { id: "delay", label: "Departure or connection delay", detail: "Late start, missed connection or a long wait", actions: ["Check time-sensitive bookings and cancellation terms.", "Keep a buffer before any critical connection.", "Confirm new departure times with the transport operator."] },
];

export default function JourneyResiliencePage() {
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [mode, setMode] = useState("car");
  const [scenario, setScenario] = useState("weather");
  const [hours, setHours] = useState("4");
  const [needs, setNeeds] = useState({ offline: true, rest: false, accessibility: false, family: false });
  const [saved, setSaved] = useState(false);
  const selected = SCENARIOS.find((item) => item.id === scenario) ?? SCENARIOS[0];
  const recommendations = useMemo(() => {
    const actions = [...selected.actions];
    if (needs.offline) actions.push("Download or write down the itinerary, key addresses and emergency contacts before departure.");
    if (needs.rest) actions.push("Plan rest breaks in advance and avoid continuing when tired.");
    if (needs.accessibility) actions.push("Verify step-free access, walking surfaces and facility accessibility with the provider; do not infer these from map data alone.");
    if (needs.family) actions.push("Agree on a meeting point and keep essential child or family travel information accessible to the responsible adults.");
    if (Number(hours) >= 6) actions.push("For this longer journey, review fuel/charging, food, rest and overnight contingencies before setting off.");
    if (mode === "transit") actions.push("Confirm the latest timetable and operator service alerts directly before travelling.");
    if (mode === "cycle" || mode === "walk") actions.push("Check route suitability for your mode, daylight and local conditions; a car route may not be safe or legal for walking/cycling.");
    return [...new Set(actions)];
  }, [selected, needs, hours, mode]);

  const exportPlan = () => {
    const lines = ["ROAMORA JOURNEY RESILIENCE PLAN", `Route: ${origin || "Origin not set"} → ${destination || "Destination not set"}`, `Mode: ${mode}`, `Scenario: ${selected.label}`, `Planned duration: ${hours} hours`, "", "PREPAREDNESS CHECKLIST", ...recommendations.map((item) => `- [ ] ${item}`), "", "IMPORTANT: This is a planning checklist based on your selections, not a live route assessment or emergency service. Verify current conditions and official guidance before travel."];
    const blob = new Blob([lines.join("\n")], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "roamora-journey-resilience-plan.txt";
    link.click();
    URL.revokeObjectURL(url);
    setSaved(true);
  };

  return (
    <main className="resilience-page">
      <section className="resilience-hero">
        <span className="resilience-eyebrow"><Sparkles size={14} /> ROAMORA / JOURNEY RESILIENCE</span>
        <h1>Plan for the journey.<br /><em>Prepare for the unexpected.</em></h1>
        <p>Build a practical fallback checklist around the kind of disruption you want to prepare for. This first version uses your inputs and general preparedness guidance—not live incident data.</p>
        <div className="resilience-trust"><ShieldCheck size={17} /> No invented live alerts. No claim that a route is guaranteed safe.</div>
      </section>

      <section className="resilience-layout">
        <form className="resilience-form" onSubmit={(event) => { event.preventDefault(); setSaved(false); }}>
          <div className="resilience-section-title"><Compass size={19} /><div><span>01 / YOUR JOURNEY</span><h2>Set the context</h2></div></div>
          <div className="resilience-field-grid">
            <label>Starting point<input value={origin} onChange={(e) => setOrigin(e.target.value)} placeholder="e.g. Nellore" maxLength={120} /></label>
            <label>Destination<input value={destination} onChange={(e) => setDestination(e.target.value)} placeholder="e.g. Bengaluru" maxLength={120} /></label>
          </div>
          <label>Travel mode<select value={mode} onChange={(e) => setMode(e.target.value)}><option value="car">Car / road trip</option><option value="transit">Bus / train / public transport</option><option value="flight">Flight</option><option value="cycle">Cycling</option><option value="walk">Walking</option></select></label>
          <label>Estimated journey duration <strong>{hours} hours</strong><input type="range" min="1" max="18" value={hours} onChange={(e) => setHours(e.target.value)} /><span className="resilience-hint">A rough planning input, not a calculated ETA.</span></label>

          <div className="resilience-section-title"><AlertTriangle size={19} /><div><span>02 / WHAT-IF PLANNING</span><h2>Choose a scenario</h2></div></div>
          <div className="resilience-scenarios">
            {SCENARIOS.map((item) => <button type="button" key={item.id} className={`resilience-scenario ${scenario === item.id ? "selected" : ""}`} onClick={() => { setScenario(item.id); setSaved(false); }} aria-pressed={scenario === item.id}><strong>{item.label}</strong><span>{item.detail}</span></button>)}
          </div>

          <div className="resilience-section-title"><CheckCircle2 size={19} /><div><span>03 / YOUR NEEDS</span><h2>Personalize the checklist</h2></div></div>
          <div className="resilience-needs">
            {[{ key: "offline", label: "I may need offline information" }, { key: "rest", label: "I need regular rest breaks" }, { key: "accessibility", label: "Accessibility needs matter for this trip" }, { key: "family", label: "I'm coordinating with family or children" }].map((item) => <label key={item.key}><input type="checkbox" checked={needs[item.key]} onChange={(e) => { setNeeds((current) => ({ ...current, [item.key]: e.target.checked })); setSaved(false); }} />{item.label}</label>)}
          </div>
          <button className="resilience-submit" type="submit">Refresh preparedness plan <ArrowRight size={17} /></button>
        </form>

        <aside className="resilience-output" aria-live="polite">
          <div className="resilience-output-top"><span>YOUR PREPAREDNESS BRIEF</span><span className="resilience-status"><i /> PLANNING MODE</span></div>
          <h2>{origin.trim() || "Your starting point"} <span>→</span> {destination.trim() || "Your destination"}</h2>
          <p className="resilience-summary">Prepare for: <strong>{selected.label}</strong>. Use this list to plan ahead; verify live conditions with the relevant official provider before acting.</p>
          <div className="resilience-meta"><span><Clock3 size={15} /> {hours} hour{Number(hours) === 1 ? "" : "s"} planned</span><span><Compass size={15} /> {mode === "transit" ? "Public transport" : mode === "car" ? "Road journey" : mode === "flight" ? "Flight" : mode === "cycle" ? "Cycling" : "Walking"}</span></div>
          <div className="resilience-checklist"><h3>Fallback checklist <small>{recommendations.length} actions</small></h3>{recommendations.map((item, index) => <div className="resilience-action" key={`${scenario}-${index}`}><span>{String(index + 1).padStart(2, "0")}</span><p>{item}</p></div>)}</div>
          <button className="resilience-download" type="button" onClick={exportPlan}><Download size={16} /> Download checklist</button>
          {saved && <p className="resilience-download-confirmation">Your checklist download was triggered.</p>}
          <p className="resilience-disclaimer">This tool does not fetch live weather, closures, transport status or emergency information. It is not a safety guarantee. In an emergency, contact local emergency services and follow official instructions.</p>
          <Link to="/planner" className="resilience-planner-link">Continue to the route planner <ArrowRight size={16} /></Link>
        </aside>
      </section>
    </main>
  );
}
