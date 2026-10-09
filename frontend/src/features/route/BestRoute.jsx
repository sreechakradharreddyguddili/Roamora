import {
  ArrowRight,
  Check,
  Clock3,
  MapPin,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

import "./BestRoute.css";

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

function BestRoute({
  analysis,
  selectedRoute,
  onRouteSelect,
}) {
  if (!analysis) {
    return null;
  }

  const routes = analysis.alternatives || [];

  const recommendedRoute =
    routes.find((route) => route.recommended) ||
    routes[0];

  const activeRoute =
    selectedRoute || recommendedRoute;

  if (!activeRoute) {
    return (
      <section className="best-route">
        <div className="best-route-empty">
          No route alternatives are available.
        </div>
      </section>
    );
  }

  return (
    <section className="best-route">
      <div className="best-route-heading">
        <div>
          <span className="best-route-eyebrow">
            04 / ROUTE OPTIONS
          </span>

          <h2>Compare your alternatives</h2>

          <p>
            Choose a route based on travel time,
            distance and weather-aware risk.
          </p>
        </div>

        <div className="best-route-heading-icon">
          <Sparkles size={20} />
        </div>
      </div>

      <div className="best-route-list">
        {routes.map((route, index) => {
          const routeKey =
            route.routeKey || `route-${index}`;

          const isSelected =
            activeRoute === route ||
            activeRoute.routeKey === route.routeKey;

          const isRecommended =
            route.recommended ||
            route === recommendedRoute;

          return (
            <button
              type="button"
              key={routeKey}
              className={`best-route-card ${
                isSelected ? "selected" : ""
              }`}
              onClick={() =>
                onRouteSelect?.(routeKey)
              }
            >
              <div className="best-route-card-top">
                <div className="best-route-number">
                  {index + 1}
                </div>

                <div className="best-route-card-title">
                  <div>
                    <strong>
                      Route {index + 1}
                    </strong>

                    {isRecommended && (
                      <span className="best-route-recommended">
                        <Check size={12} />
                        Recommended
                      </span>
                    )}
                  </div>

                  <span>
                    {route.riskLevel
                      ? String(
                          route.riskLevel
                        ).replaceAll("_", " ")
                      : "Risk assessment available"}
                  </span>
                </div>

                <div className="best-route-select">
                  {isSelected ? (
                    <Check size={18} />
                  ) : (
                    <ArrowRight size={18} />
                  )}
                </div>
              </div>

              <div className="best-route-stats">
                <div>
                  <MapPin size={15} />
                  <span>
                    {route.distanceKm !==
                      undefined &&
                    route.distanceKm !== null
                      ? `${Number(
                          route.distanceKm
                        ).toFixed(1)} km`
                      : "Distance —"}
                  </span>
                </div>

                <div>
                  <Clock3 size={15} />
                  <span>
                    {formatDuration(
                      route.durationMinutes
                    )}
                  </span>
                </div>

                <div>
                  <ShieldCheck size={15} />
                  <span>
                    Risk{" "}
                    {route.riskScore !==
                      undefined &&
                    route.riskScore !== null
                      ? `${route.riskScore}/100`
                      : "—"}
                  </span>
                </div>
              </div>

              {route.recommendation && (
                <p className="best-route-recommendation">
                  {route.recommendation}
                </p>
              )}
            </button>
          );
        })}
      </div>

      <div className="best-route-active">
        <div>
          <span>SELECTED ROUTE</span>
          <strong>
            Route{" "}
            {Math.max(
              1,
              routes.indexOf(activeRoute) + 1
            )}
          </strong>
        </div>

        <div>
          <span>TIME</span>
          <strong>
            {formatDuration(
              activeRoute.durationMinutes
            )}
          </strong>
        </div>

        <div>
          <span>DISTANCE</span>
          <strong>
            {activeRoute.distanceKm !==
              undefined &&
            activeRoute.distanceKm !== null
              ? `${Number(
                  activeRoute.distanceKm
                ).toFixed(1)} km`
              : "—"}
          </strong>
        </div>
      </div>
    </section>
  );
}

export default BestRoute;