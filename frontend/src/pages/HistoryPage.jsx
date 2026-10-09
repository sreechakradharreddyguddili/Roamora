import { useEffect, useMemo, useState } from "react";
import {
  Bookmark,
  BookmarkCheck,
  CalendarDays,
  Clock3,
  MapPin,
  Route,
  Search,
  ShieldCheck,
  Trash2,
  X,
} from "lucide-react";

import {
  clearRouteHistory,
  deleteRoute,
  getRoutes,
  getSavedRoutes,
  saveRoute,
  unsaveRoute,
} from "../api/routeApi";

import "./HistoryPage.css";

function formatDuration(minutes) {
  if (
    minutes === null ||
    minutes === undefined ||
    Number.isNaN(Number(minutes))
  ) {
    return "—";
  }

  const value = Math.round(Number(minutes));
  const hours = Math.floor(value / 60);
  const mins = value % 60;

  if (hours === 0) {
    return `${mins} min`;
  }

  if (mins === 0) {
    return `${hours} hr`;
  }

  return `${hours} hr ${mins} min`;
}

function formatDate(value) {
  if (!value) {
    return "Recently";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Recently";
  }

  return date.toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function formatTransport(value) {
  if (!value) {
    return "Car";
  }

  return String(value)
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [savedRoutes, setSavedRoutes] = useState([]);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState("");

  const [activeTab, setActiveTab] = useState("history");
  const [search, setSearch] = useState("");
  const [selectedRoute, setSelectedRoute] =
    useState(null);

  useEffect(() => {
    let mounted = true;

    async function loadRoutes() {
      try {
        setError("");
        const [historyPage, savedPage] =
          await Promise.all([
            getRoutes(0, 100),
            getSavedRoutes(0, 100),
          ]);

        if (!mounted) {
          return;
        }

        const historyData =
          historyPage?.content || [];

        const savedData =
          savedPage?.content || [];

        setHistory(historyData);
        setSavedRoutes(savedData);
      } catch (error) {
        console.error(
          "Unable to load route history:",
          error
        );

        if (mounted) {
          setHistory([]);
          setSavedRoutes([]);
          setError(error.message || "Unable to load your journeys right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    loadRoutes();

    return () => {
      mounted = false;
    };
  }, []);

  const filteredHistory = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) {
      return history;
    }

    return history.filter((route) =>
      [
        route.origin,
        route.destination,
        route.transportMode,
        route.status,
        route.riskLevel,
        route.recommendation,
      ]
        .filter(Boolean)
        .some((value) =>
          String(value)
            .toLowerCase()
            .includes(query)
        )
    );
  }, [history, search]);

  const filteredSavedRoutes = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) {
      return savedRoutes;
    }

    return savedRoutes.filter((route) =>
      [
        route.origin,
        route.destination,
        route.transportMode,
        route.status,
        route.riskLevel,
        route.recommendation,
      ]
        .filter(Boolean)
        .some((value) =>
          String(value)
            .toLowerCase()
            .includes(query)
        )
    );
  }, [savedRoutes, search]);

  async function handleDeleteHistory(id) {
    if (!id || actionLoading) {
      return;
    }

    try {
      setActionLoading(`delete-${id}`);

      await deleteRoute(id);

      setHistory((current) =>
        current.filter(
          (route) => route.id !== id
        )
      );

      setSavedRoutes((current) =>
        current.filter(
          (route) => route.id !== id
        )
      );

      if (selectedRoute?.id === id) {
        setSelectedRoute(null);
      }
    } catch (error) {
      console.error(
        "Unable to delete route:",
        error
      );
      setError(error.message || "Unable to delete this journey.");
    } finally {
      setActionLoading(null);
    }
  }

  async function handleClearHistory() {
    if (
      history.filter((route) => !route.saved)
        .length === 0 ||
      actionLoading
    ) {
      return;
    }

    try {
      setActionLoading("clear-history");

      await clearRouteHistory();

      setHistory((current) =>
        current.filter((route) => route.saved)
      );
    } catch (error) {
      console.error(
        "Unable to clear route history:",
        error
      );
      setError(error.message || "Unable to clear route history.");
    } finally {
      setActionLoading(null);
    }
  }

  async function handleToggleSaved(route) {
    if (!route?.id || actionLoading) {
      return;
    }

    try {
      setActionLoading(`save-${route.id}`);

      if (route.saved) {
        await unsaveRoute(route.id);

        setHistory((current) =>
          current.map((item) =>
            item.id === route.id
              ? {
                  ...item,
                  saved: false,
                }
              : item
          )
        );

        setSavedRoutes((current) =>
          current.filter(
            (item) => item.id !== route.id
          )
        );
      } else {
        const result =
          await saveRoute(route.id);

        const savedRoute =
          result?.data || route;

        const updatedRoute = {
          ...route,
          ...(savedRoute || {}),
          saved: true,
        };

        setHistory((current) =>
          current.map((item) =>
            item.id === route.id
              ? updatedRoute
              : item
          )
        );

        setSavedRoutes((current) => {
          const exists = current.some(
            (item) => item.id === route.id
          );

          if (exists) {
            return current.map((item) =>
              item.id === route.id
                ? updatedRoute
                : item
            );
          }

          return [
            updatedRoute,
            ...current,
          ];
        });
      }
    } catch (error) {
      console.error(
        "Unable to update saved route:",
        error
      );
      setError(error.message || "Unable to update this saved route.");
    } finally {
      setActionLoading(null);
    }
  }

  const routes =
    activeTab === "history"
      ? filteredHistory
      : filteredSavedRoutes;

  const unsavedHistoryCount =
    history.filter((route) => !route.saved)
      .length;

  return (
    <main className="history-page">
      <section className="history-hero">
        <div>
          <span className="history-eyebrow">
            ROAMORA JOURNEY ARCHIVE
          </span>

          <h1>
            Your route
            <br />
            history.
          </h1>

          <p>
            Review journeys you've planned and routes
            you've saved for later.
          </p>
        </div>

        <div className="history-count">
          <strong>
            {activeTab === "history"
              ? history.length
              : savedRoutes.length}
          </strong>

          <span>
            {activeTab === "history"
              ? "journeys"
              : "saved routes"}
          </span>
        </div>
      </section>

      <section className="history-toolbar">
        <div className="history-tabs">
          <button
            type="button"
            className={
              activeTab === "history"
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveTab("history")
            }
          >
            <Route size={16} />
            History
            <span>{history.length}</span>
          </button>

          <button
            type="button"
            className={
              activeTab === "saved"
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveTab("saved")
            }
          >
            <Bookmark size={16} />
            Saved
            <span>{savedRoutes.length}</span>
          </button>
        </div>

        <div className="history-search">
          <Search size={17} />

          <input
            type="text"
            value={search}
            onChange={(event) =>
              setSearch(event.target.value)
            }
            placeholder="Search journeys..."
          />

          {search && (
            <button
              type="button"
              onClick={() => setSearch("")}
              aria-label="Clear search"
            >
              <X size={15} />
            </button>
          )}
        </div>
      </section>

      {error && (
        <div className="history-error" role="alert">
          <strong>Journey archive is temporarily unavailable.</strong>
          <span>{error}</span>
        </div>
      )}

      {activeTab === "history" &&
        history.length > 0 && (
          <div className="history-actions">
            <button
              type="button"
              onClick={handleClearHistory}
              disabled={
                unsavedHistoryCount === 0 ||
                actionLoading === "clear-history"
              }
            >
              <Trash2 size={15} />
              {actionLoading === "clear-history"
                ? "Clearing..."
                : "Clear history"}
            </button>
          </div>
        )}

      {loading ? (
        <section className="history-empty">
          <div className="history-empty-icon">
            <Route size={24} />
          </div>

          <span>LOADING</span>

          <h2>
            Loading your journeys.
          </h2>

          <p>
            Fetching your route history from Roamora.
          </p>
        </section>
      ) : routes.length > 0 ? (
        <section className="history-list">
          {routes.map((route, index) => {
            const saved = Boolean(route.saved);

            const riskLevel =
              route.riskLevel ||
              route.status ||
              "Unknown";

            return (
              <article
                className="history-card"
                key={
                  route.id ||
                  `${route.origin}-${route.destination}-${index}`
                }
              >
                <div className="history-card-index">
                  {String(index + 1).padStart(2, "0")}
                </div>

                <div className="history-card-main">
                  <div className="history-route">
                    <div className="history-place">
                      <span>FROM</span>

                      <strong>
                        {route.origin ||
                          "Unknown"}
                      </strong>
                    </div>

                    <div className="history-route-line">
                      <div />
                      <MapPin size={15} />
                      <div />
                    </div>

                    <div className="history-place">
                      <span>TO</span>

                      <strong>
                        {route.destination ||
                          "Unknown"}
                      </strong>
                    </div>
                  </div>

                  <div className="history-meta">
                    <span>
                      {formatTransport(
                        route.transportMode
                      )}
                    </span>

                    <span>
                      {route.distanceKm !==
                        undefined &&
                      route.distanceKm !== null
                        ? `${Number(
                            route.distanceKm
                          ).toFixed(1)} km`
                        : "Distance —"}
                    </span>

                    <span>
                      <Clock3 size={13} />
                      {formatDuration(
                        route.durationMinutes
                      )}
                    </span>

                    <span>
                      <CalendarDays size={13} />
                      {formatDate(
                        route.createdAt
                      )}
                    </span>
                  </div>
                </div>

                <div className="history-card-side">
                  <div
                    className={`history-risk ${String(
                      riskLevel
                    ).toLowerCase()}`}
                  >
                    <ShieldCheck size={14} />

                    <span>
                      {riskLevel}
                    </span>

                    {route.riskScore !==
                      undefined &&
                      route.riskScore !== null && (
                        <strong>
                          {route.riskScore}
                        </strong>
                      )}
                  </div>

                  <div className="history-card-actions">
                    <button
                      type="button"
                      className={
                        saved ? "saved" : ""
                      }
                      onClick={() =>
                        handleToggleSaved(route)
                      }
                      disabled={
                        actionLoading ===
                        `save-${route.id}`
                      }
                      aria-label={
                        saved
                          ? "Remove saved route"
                          : "Save route"
                      }
                      title={
                        saved
                          ? "Remove saved route"
                          : "Save route"
                      }
                    >
                      {saved ? (
                        <BookmarkCheck size={16} />
                      ) : (
                        <Bookmark size={16} />
                      )}
                    </button>

                    {activeTab === "history" && (
                      <button
                        type="button"
                        onClick={() =>
                          handleDeleteHistory(
                            route.id
                          )
                        }
                        disabled={
                          actionLoading ===
                          `delete-${route.id}`
                        }
                        aria-label="Delete history item"
                        title="Delete"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}

                    <button
                      type="button"
                      onClick={() =>
                        setSelectedRoute(route)
                      }
                      className="details-button"
                    >
                      Details
                    </button>
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      ) : (
        <section className="history-empty">
          <div className="history-empty-icon">
            {activeTab === "saved" ? (
              <Bookmark size={24} />
            ) : (
              <Route size={24} />
            )}
          </div>

          <span>
            {search
              ? "NO MATCHES"
              : activeTab === "saved"
                ? "NO SAVED ROUTES"
                : "NO JOURNEY HISTORY"}
          </span>

          <h2>
            {search
              ? "Nothing matched your search."
              : activeTab === "saved"
                ? "Save routes you want to revisit."
                : "Your journeys will appear here."}
          </h2>

          <p>
            {search
              ? "Try a different city, route, transport mode or risk level."
              : activeTab === "saved"
                ? "Use the bookmark action on a route to keep it available for later."
                : "Plan a journey to start building your route history."}
          </p>
        </section>
      )}

      {selectedRoute && (
        <div
          className="history-modal-backdrop"
          onMouseDown={(event) => {
            if (
              event.target === event.currentTarget
            ) {
              setSelectedRoute(null);
            }
          }}
        >
          <section
            className="history-modal"
            role="dialog"
            aria-modal="true"
            aria-label="Route details"
          >
            <div className="history-modal-header">
              <div>
                <span>ROUTE DETAILS</span>

                <h2>
                  {selectedRoute.origin}
                  <br />
                  <small>to</small>{" "}
                  {selectedRoute.destination}
                </h2>
              </div>

              <button
                type="button"
                onClick={() =>
                  setSelectedRoute(null)
                }
                aria-label="Close details"
              >
                <X size={19} />
              </button>
            </div>

            <div className="history-modal-stats">
              <div>
                <span>TRANSPORT</span>

                <strong>
                  {formatTransport(
                    selectedRoute.transportMode
                  )}
                </strong>
              </div>

              <div>
                <span>DISTANCE</span>

                <strong>
                  {selectedRoute.distanceKm !==
                    undefined &&
                  selectedRoute.distanceKm !== null
                    ? `${Number(
                        selectedRoute.distanceKm
                      ).toFixed(1)} km`
                    : "—"}
                </strong>
              </div>

              <div>
                <span>DURATION</span>

                <strong>
                  {formatDuration(
                    selectedRoute.durationMinutes
                  )}
                </strong>
              </div>

              <div>
                <span>RISK</span>

                <strong>
                  {selectedRoute.riskScore !==
                    undefined &&
                  selectedRoute.riskScore !== null
                    ? `${selectedRoute.riskScore}/100`
                    : selectedRoute.riskLevel ||
                      selectedRoute.status ||
                      "—"}
                </strong>
              </div>
            </div>

            {selectedRoute.recommendation && (
              <div className="history-modal-recommendation">
                <ShieldCheck size={18} />

                <div>
                  <span>RECOMMENDATION</span>

                  <p>
                    {selectedRoute.recommendation}
                  </p>
                </div>
              </div>
            )}

            <div className="history-modal-footer">
              <span>
                Planned{" "}
                {formatDate(
                  selectedRoute.createdAt
                )}
              </span>

              <button
                type="button"
                onClick={() =>
                  setSelectedRoute(null)
                }
              >
                Close
              </button>
            </div>
          </section>
        </div>
      )}
    </main>
  );
}

export default HistoryPage;