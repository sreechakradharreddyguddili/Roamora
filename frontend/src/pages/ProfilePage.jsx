import { useEffect, useMemo, useState } from "react";
import {
  Bell,
  CalendarDays,
  Check,
  LogOut,
  Mail,
  ShieldCheck,
  Sparkles,
  UserRound,
} from "lucide-react";

import { getProfile } from "../api/profileApi";
import { getRoutes } from "../api/routeApi";
import { storage } from "../utils/storage";

import "./ProfilePage.css";

function initials(name = "Roamora traveler") {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || "")
    .join("") || "R";
}

function formatMemberSince(value) {
  if (!value) return "Recently joined";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Recently joined";
  return new Intl.DateTimeFormat(undefined, {
    month: "long",
    year: "numeric",
  }).format(date);
}

export default function ProfilePage({ session, onLogout }) {
  const [profile, setProfile] = useState(null);
  const [routeCount, setRouteCount] = useState(0);
  const [preferences, setPreferences] = useState(() => storage.getPreferences());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function load() {
      try {
        setLoading(true);
        setError("");

        const [profileData, routes] = await Promise.all([
          getProfile(),
          getRoutes(0, 1),
        ]);

        if (!mounted) return;

        setProfile(profileData);
        setRouteCount(Number(routes?.totalElements || 0));
      } catch (requestError) {
        if (!mounted) return;
        setError(requestError.message || "Unable to load your profile.");
        setProfile(null);
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const user = profile || session || {};
  const name = user.name || "Roamora traveler";
  const email = user.email || "";
  const memberSince = formatMemberSince(user.createdAt);
  const savedCount = storage.getSaved().length;

  const preferenceLabels = useMemo(() => ({
    riskTolerance: {
      relaxed: "Relaxed",
      balanced: "Balanced",
      cautious: "Cautious",
    },
    optimization: {
      fastest: "Fastest",
      balanced: "Balanced",
      comfort: "Comfort",
      scenic: "Scenic",
    },
  }), []);

  const updatePreference = (key, value) => {
    const next = { ...preferences, [key]: value };
    setPreferences(next);
    storage.setPreferences(next);
    setSaved(true);
    window.setTimeout(() => setSaved(false), 1400);
  };

  const toggleNotifications = () => {
    updatePreference("notifications", !preferences.notifications);
  };

  return (
    <main className="profile-page">
      <section className="profile-hero">
        <span className="profile-eyebrow">ROAMORA ACCOUNT</span>
        <h1>Your travel<br /><em>control room.</em></h1>
        <p>
          Manage your account, journey preferences and travel intelligence settings in one place.
        </p>
      </section>

      {error && (
        <div className="profile-error" role="alert">
          <strong>We could not refresh your account.</strong>
          <span>{error}</span>
        </div>
      )}

      <div className="profile-layout">
        <div className="profile-main">
          <section className="profile-card">
            <div className="profile-card-heading">
              <div className="profile-heading-icon"><UserRound size={18} /></div>
              <div>
                <span>ACCOUNT</span>
                <h2>Profile details</h2>
              </div>
            </div>

            <div className="profile-user">
              <div className="profile-avatar">{initials(name)}</div>
              <div>
                <strong>{loading ? "Loading your profile..." : name}</strong>
                <span><Mail size={12} /> {email || "Email unavailable"}</span>
              </div>
            </div>

            <div className="profile-info-grid">
              <div>
                <span>MEMBER SINCE</span>
                <strong>{memberSince}</strong>
              </div>
              <div>
                <span>ACCOUNT TYPE</span>
                <strong>{String(user.role || "USER").toLowerCase()}</strong>
              </div>
              <div>
                <span>JOURNEYS PLANNED</span>
                <strong>{routeCount}</strong>
              </div>
              <div>
                <span>SAVED ROUTES</span>
                <strong>{savedCount}</strong>
              </div>
            </div>
          </section>

          <section className="profile-card">
            <div className="profile-card-heading">
              <div className="profile-heading-icon"><Sparkles size={18} /></div>
              <div>
                <span>INTELLIGENCE</span>
                <h2>Journey preferences</h2>
              </div>
            </div>

            <div className="profile-preference">
              <div>
                <strong>Risk tolerance</strong>
                <p>How strongly Roamora should surface risk context when comparing routes.</p>
              </div>
              <div className="profile-segmented">
                {Object.entries(preferenceLabels.riskTolerance).map(([value, label]) => (
                  <button
                    type="button"
                    key={value}
                    className={preferences.riskTolerance === value ? "active" : ""}
                    onClick={() => updatePreference("riskTolerance", value)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-preference">
              <div>
                <strong>Route priority</strong>
                <p>Choose the kind of trade-off you want to start with when alternatives are available.</p>
              </div>
              <div className="profile-segmented">
                {Object.entries(preferenceLabels.optimization).map(([value, label]) => (
                  <button
                    type="button"
                    key={value}
                    className={preferences.optimization === value ? "active" : ""}
                    onClick={() => updatePreference("optimization", value)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="profile-preference">
              <div className="profile-toggle-row">
                <div>
                  <strong>Travel notifications</strong>
                  <p>Keep useful journey updates enabled on this device.</p>
                </div>
              </div>
              <button
                type="button"
                className={`profile-toggle ${preferences.notifications ? "active" : ""}`}
                onClick={toggleNotifications}
                aria-pressed={preferences.notifications}
                aria-label="Toggle travel notifications"
              >
                <span />
              </button>
            </div>

            {saved && (
              <div className="profile-saved" role="status">
                <Check size={14} /> Preferences saved on this device.
              </div>
            )}
          </section>
        </div>

        <aside className="profile-sidebar">
          <div className="profile-status-card">
            <div className="profile-status-icon"><ShieldCheck size={19} /></div>
            <span>ROAMORA STATUS</span>
            <strong>Ready to roam.</strong>
            <p>Your account is connected and your planning preferences are available for the Roamora experience.</p>
            <div className="profile-status-line">
              <span>Session</span>
              <strong>Active</strong>
            </div>
          </div>

          <div className="profile-save-card">
            <CalendarDays size={17} />
            <div>
              <strong>Plan before you move</strong>
              <p>Use the Planner for route, weather and risk context before departure.</p>
            </div>
          </div>

          <div className="profile-save-card">
            <Bell size={17} />
            <div>
              <strong>Preferences are local</strong>
              <p>Your UI preferences are stored on this device so Roamora stays fast and predictable.</p>
            </div>
          </div>

          <button type="button" className="profile-logout" onClick={onLogout}>
            <LogOut size={15} />
            Sign out of Roamora
          </button>
        </aside>
      </div>
    </main>
  );
}
