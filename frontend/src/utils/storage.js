const KEYS = {
  session: "roamora.session",
  history: "roamora.history",
  saved: "roamora.savedRoutes",
  preferences: "roamora.preferences",
};

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function write(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
  window.dispatchEvent(new CustomEvent("roamora:storage", { detail: { key } }));
}

export const storage = {
  getSession: () => read(KEYS.session, null),
  setSession: (session) => write(KEYS.session, session),
  clearSession: () => {
    localStorage.removeItem(KEYS.session);
    window.dispatchEvent(new CustomEvent("roamora:storage", { detail: { key: KEYS.session } }));
  },

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
