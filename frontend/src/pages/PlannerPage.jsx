import { useCallback, useState } from "react";
import {
  Bookmark,
  BookmarkCheck,
  History,
  Share2,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

import SearchForm from "../features/route/SearchForm";
import RouteAnalysis from "../features/route/RouteAnalysis";
import BestRoute from "../features/route/BestRoute";
import MapView from "../features/route/MapView";

import {
  planRoute,
  saveRoute,
  unsaveRoute,
} from "../api/routeApi";


import "./PlannerPage.css";
import { handleImageError } from "../utils/imageFallback";

function PlannerPage() {
  const navigate = useNavigate();

  const [analysis, setAnalysis] = useState(null);
  const [locations, setLocations] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [selectedRouteKey, setSelectedRouteKey] =
    useState(null);
  const [saved, setSaved] = useState(false);

  const handleSearch = useCallback(
    async (
      origin,
      destination,
      transportMode,
      departureDate,
      departureTime,
      originLocation,
      destinationLocation
    ) => {
      setLoading(true);
      setError("");
      setNotice("");
      setAnalysis(null);
      setLocations(null);
      window.dispatchEvent(new CustomEvent("roamora:analysis", { detail: null }));
      setSelectedRouteKey(null);
      setSaved(false);

      const normalizedTransportMode =
        transportMode?.toUpperCase() || "CAR";

      try {
        const routeAnalysis =
          await planRoute({
            origin,
            destination,
            originLocation,
            destinationLocation,
            transportMode:
              normalizedTransportMode,
            departureDate,
            departureTime,
          });

        if (!routeAnalysis) {
          throw new Error(
            "Unable to calculate the route."
          );
        }

        setAnalysis(routeAnalysis);
        window.dispatchEvent(new CustomEvent("roamora:analysis", {
          detail: {
            analysis: routeAnalysis,
            locations: { origin, destination, transportMode: normalizedTransportMode, departureDate, departureTime },
          },
        }));

        setLocations({
          origin,
          destination,

          originLocation:
            originLocation || null,

          destinationLocation:
            destinationLocation || null,

          transportMode:
            normalizedTransportMode,

          departureDate,
          departureTime,
        });

        const recommendedRoute =
          routeAnalysis?.alternativeRoutes?.find(
            (route) => route.recommended
          ) ||
          routeAnalysis?.alternatives?.find(
            (route) => route.recommended
          ) ||
          routeAnalysis?.alternativeRoutes?.[0] ||
          routeAnalysis?.alternatives?.[0];

        if (recommendedRoute?.routeKey) {
          setSelectedRouteKey(
            recommendedRoute.routeKey
          );
        }
      } catch (requestError) {
        console.error(
          "Roamora route planning failed:",
          requestError
        );

        setError(
          requestError.message ||
            "Route not found. Please check your locations and try again."
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  const handleRouteSelect = useCallback(
    (routeKeyValue) => {
      setSelectedRouteKey(routeKeyValue);
    },
    []
  );

  const getSelectedRoute = useCallback(() => {
    if (!analysis) {
      return null;
    }

    return (
      analysis?.alternativeRoutes?.find(
        (route) =>
          route.routeKey === selectedRouteKey
      ) ||
      analysis?.alternatives?.find(
        (route) =>
          route.routeKey === selectedRouteKey
      ) ||
      analysis?.alternativeRoutes?.find(
        (route) => route.recommended
      ) ||
      analysis?.alternatives?.find(
        (route) => route.recommended
      ) ||
      analysis?.alternativeRoutes?.[0] ||
      analysis?.alternatives?.[0] ||
      null
    );
  }, [analysis, selectedRouteKey]);

  const handleSaveRoute = async () => {
    if (!analysis?.routeId || saving) {
      return;
    }

    try {
      setSaving(true);
      setError("");
      setNotice("");

      if (saved) {
        await unsaveRoute(analysis.routeId);
        setSaved(false);
      } else {
        await saveRoute(analysis.routeId);
        setSaved(true);
      }
    } catch (requestError) {
      console.error(requestError);

      setError(
        requestError.message ||
          "Unable to update the saved route."
      );
    } finally {
      setSaving(false);
    }
  };

  const handleHistorySave = () => {
    navigate("/history");
  };

  const openJourneyWorkspace = () => {
    if (!analysis || !locations) return;
    navigate("/journey-workspace", { state: { analysis, locations } });
  };

  const handleShareRoute = async () => {
    if (!locations) return;

    const title = `Roamora journey: ${locations.origin} → ${locations.destination}`;
    const text = selectedRoute
      ? `${title} • ${selectedRoute.distanceKm ? `${Number(selectedRoute.distanceKm).toFixed(1)} km` : "route ready"} • Roamora route intelligence`
      : `${title} • Roamora route intelligence`;

    try {
      if (navigator.share) {
        await navigator.share({ title, text, url: window.location.href });
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(`${text}\n${window.location.href}`);
        setNotice("Journey details copied to your clipboard.");
      }
    } catch (shareError) {
      if (shareError?.name !== "AbortError") {
        setError("Unable to share this journey from the current browser.");
      }
    }
  };

  const selectedRoute = getSelectedRoute();

  return (
    <main className="planner-page">
      {/* =====================================================
          HERO
      ===================================================== */}

      <section className="planner-hero">
        <div className="planner-hero-content">
          <span className="planner-eyebrow">
            <span className="planner-eyebrow-dot" />
            ROAMORA ROUTE INTELLIGENCE
          </span>

          <h1>
            Plan the journey.
            <br />
            <em>Understand the road.</em>
          </h1>

          <p>
            Compare routes, understand weather conditions,
            elevation and travel risk before you start moving.
          </p>

          <div className="planner-hero-meta">
            <span>
              <i />
              Weather aware
            </span>

            <span>
              <i />
              Route intelligence
            </span>

            <span>
              <i />
              Risk context
            </span>
          </div>
        </div>

        {/* =================================================
            HERO COMPASS
        ================================================= */}

        <div className="planner-hero-visual planner-photo-visual">
          <img src="https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=82" alt="Mountain range overlooking a scenic travel route" loading="lazy" onError={handleImageError} />
          <div className="planner-photo-caption"><span>JOURNEY STATUS</span><strong>Ready when you are.</strong><small>Choose your route to see the details Roamora can verify.</small></div>
        </div>
      </section>

      {/* =====================================================
          SEARCH
      ===================================================== */}

      <section className="planner-search-section">
        <div className="planner-section-heading">
          <div>
            <span className="planner-section-label">
              01 / JOURNEY
            </span>

            <h2>
              Where are you <em>going?</em>
            </h2>
          </div>

          <p>
            Enter your starting point, destination and
            travel details. Roamora will analyze the
            journey before you leave.
          </p>
        </div>

        <div className="planner-search-wrapper">
          <SearchForm
            onSearch={handleSearch}
            loading={loading}
          />
        </div>
      </section>

      {/* =====================================================
          ERROR
      ===================================================== */}

      {notice && (
        <div className="planner-notice" role="status">
          <strong>Roamora</strong>
          <span>{notice}</span>
        </div>
      )}

      {error && (
        <div
          className="planner-error"
          role="alert"
        >
          <div className="planner-error-icon">
            !
          </div>

          <div>
            <strong>
              Unable to process this route
            </strong>

            <span>{error}</span>
          </div>
        </div>
      )}

      {/* =====================================================
          LOADING
      ===================================================== */}

      {loading && (
        <section className="planner-loading">
          <div className="planner-loading-visual">
            <div className="planner-loading-ring" />
            <div className="planner-loading-dot" />
          </div>

          <div>
            <span className="planner-loading-label">
              ROAMORA IS THINKING
            </span>

            <strong>
              Analyzing your journey...
            </strong>

            <p>
              Checking routes, weather, schedules and
              travel risk.
            </p>
          </div>
        </section>
      )}

      {/* =====================================================
          RESULTS
      ===================================================== */}

      {locations && analysis && (
        <div className="planner-results">
          {/* =================================================
              MAP
          ================================================= */}

          <section className="planner-map-section">
            <div className="planner-section-heading">
              <div>
                <span className="planner-section-label">
                  02 / ROUTE MAP
                </span>

                <h2>
                  Your <em>journey.</em>
                </h2>
              </div>

              <p>
                Select a route to inspect its details
                and compare the available journey options.
              </p>
            </div>

            <div className="planner-map-wrapper">
              <MapView
                analysis={analysis}
                locations={locations}
                selectedRouteKey={selectedRouteKey}
                onRouteSelect={handleRouteSelect}
              />
            </div>
          </section>

          {/* =================================================
              ACTIONS
          ================================================= */}

          <section className="planner-actions-section">
            <div className="planner-result-status">
              <span className="planner-status-dot" />

              <div>
                <small>
                  ROUTE ANALYSIS
                </small>

                <strong>
                  Journey intelligence ready
                </strong>
              </div>
            </div>

            <div className="planner-actions">
              <button
                type="button"
                className="planner-action-button primary"
                onClick={handleSaveRoute}
                disabled={
                  saving || !analysis?.routeId
                }
              >
                {saved ? (
                  <BookmarkCheck size={18} />
                ) : (
                  <Bookmark size={18} />
                )}

                {saving
                  ? "Updating..."
                  : saved
                    ? "Route saved"
                    : "Save route"}
              </button>

              <button
                type="button"
                className="planner-action-button secondary"
                onClick={openJourneyWorkspace}
              >
                Open journey workspace
              </button>

              <button
                type="button"
                className="planner-action-button secondary"
                onClick={handleHistorySave}
              >
                <History size={18} />
                View history
              </button>

              <button
                type="button"
                className="planner-action-button secondary"
                onClick={handleShareRoute}
              >
                <Share2 size={18} />
                Share journey
              </button>
            </div>
          </section>

          {/* =================================================
              CONDITIONS
          ================================================= */}

          <section className="planner-analysis-section">
            <div className="planner-section-heading">
              <div>
                <span className="planner-section-label">
                  03 / CONDITIONS
                </span>

                <h2>
                  Understand the <em>road.</em>
                </h2>
              </div>

              <p>
                Weather and risk information connected
                to your selected route.
              </p>
            </div>

            <div className="planner-analysis-wrapper">
              <RouteAnalysis
                analysis={analysis}
                selectedRoute={selectedRoute}
              />
            </div>
          </section>

          {/* =================================================
              BEST ROUTE
          ================================================= */}

          <section className="planner-best-route-section">
            <div className="planner-section-heading">
              <div>
                <span className="planner-section-label">
                  04 / DECISION
                </span>

                <h2>
                  Choose with <em>context.</em>
                </h2>
              </div>

              <p>
                Compare the available routes and decide
                what matters most for your journey.
              </p>
            </div>

            <BestRoute
              analysis={analysis}
              selectedRoute={selectedRoute}
              onRouteSelect={handleRouteSelect}
            />
          </section>
        </div>
      )}


      {/* =====================================================
          EMPTY STATE
      ===================================================== */}

      {!loading && !analysis && !error && (
        <section className="planner-empty">
          <div className="planner-empty-visual planner-empty-photo">
            <img src="https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=900&q=82" alt="Mountain landscape travel inspiration" loading="lazy" onError={handleImageError} />
          </div>

          <span className="planner-section-label">
            READY WHEN YOU ARE
          </span>

          <h2 className="planner-journey-title">
            <span className="journey-word word-start">
              Start
            </span>

            <span className="journey-word word-with">
              with
            </span>

            <span className="journey-word word-your">
              your
            </span>

            <span className="journey-word word-journey">
              journey.
            </span>
          </h2>

          <p>
            Enter your origin and destination above to
            generate weather-aware route intelligence.
          </p>
        </section>
      )}
    </main>
  );
}

export default PlannerPage;
