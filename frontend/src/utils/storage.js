const KEYS = {
  session: "roamora.session",
  history: "roamora.history",
  saved: "roamora.savedRoutes",
  preferences: "roamora.preferences",
};

function read(key, fallback) {
  try {
    if (typeof window === "undefined" || !window.localStorage) return fallback;
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    // Storage can be blocked by privacy settings or corrupted browser data.
    return fallback;
  }
}

function notifyStorage(key) {
  if (typeof window !== "undefined" && typeof window.dispatchEvent === "function") {
    window.dispatchEvent(new CustomEvent("roamora:storage", { detail: { key } }));
  }
}

function write(key, value) {
  try {
    if (typeof window === "undefined" || !window.localStorage) return false;
    window.localStorage.setItem(key, JSON.stringify(value));
    notifyStorage(key);
    return true;
  } catch {
    // Quota errors and browser privacy restrictions should not crash the UI.
    return false;
  }
}

function remove(key) {
  try {
    if (typeof window !== "undefined" && window.localStorage) {
      window.localStorage.removeItem(key);
    }
  } catch {
    // Best-effort cleanup; in-memory UI state is still cleared by the caller.
  }
  notifyStorage(key);
}

export const storage = {
  getSession: () => read(KEYS.session, null),
  setSession: (session) => write(KEYS.session, session),
  clearSession: () => remove(KEYS.session),

  getHistory: () => read(KEYS.history, []),
  addHistory: (item) => {
    const history = read(KEYS.history, []);
    const next = [item, ...history.filter((entry) => entry.id !== item.id)].slice(0, 50);
    write(KEYS.history, next);
    return next;
  },
  removeHistory: (id) => {
    const next = read(KEYS.history, []).filter((entry) => entry.id !== id);
    write(KEYS.history, next);
    return next;
  },
  clearHistory: () => write(KEYS.history, []),

  getSaved: () => read(KEYS.saved, []),
  toggleSaved: (route) => {
    const saved = read(KEYS.saved, []);
    const exists = saved.some((entry) => entry.key === route.key);
    const next = exists
      ? saved.filter((entry) => entry.key !== route.key)
      : [route, ...saved].slice(0, 30);
    write(KEYS.saved, next);
    return next;
  },

  getPreferences: () =>
    read(KEYS.preferences, {
      riskTolerance: "balanced",
      optimization: "balanced",
      notifications: true,
    }),
  setPreferences: (preferences) => write(KEYS.preferences, preferences),
};

export const routeKey = (origin, destination, transportMode) =>
  `${origin.trim().toLowerCase()}__${destination.trim().toLowerCase()}__${transportMode}`;
