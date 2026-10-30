import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  Bookmark,
  Clock3,
  Map,
  Plus,
  Route,
  ShieldCheck,
  TrendingUp,
} from "lucide-react";
import { Link } from "react-router-dom";

import { getRoutes } from "../api/routeApi";
import "./DashboardPage.css";

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

function DashboardPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    async function loadRoutes() {
      try {
        setError("");
        const page = await getRoutes(0, 100);

        if (mounted) {
          setHistory(page?.content || []);
        }
      } catch (error) {
        console.error(
          "Unable to load dashboard routes:",
          error
        );

        if (mounted) {
          setHistory([]);
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

  const completedRoutes = useMemo(
    () =>
      history.filter(
        (route) =>
          (route?.distanceKm !== null &&
            route?.distanceKm !== undefined) ||
          (route?.durationMinutes !== null &&
            route?.durationMinutes !== undefined)
      ),
    [history]
  );

  const stats = useMemo(() => {
    const totalDistance = completedRoutes.reduce(
      (total, route) =>
        total + Number(route.distanceKm || 0),
      0
    );

    const totalMinutes = completedRoutes.reduce(
      (total, route) =>
        total + Number(route.durationMinutes || 0),
      0
    );

    const riskScores = completedRoutes
      .map((route) => Number(route.riskScore))
      .filter((score) => !Number.isNaN(score));

    const averageRisk =
      riskScores.length > 0
        ? Math.round(
            riskScores.reduce(
              (sum, score) => sum + score,
              0
            ) / riskScores.length
          )
        : null;

    const savedRoutes = completedRoutes.filter(
      (route) => route.saved
    ).length;

    return {
      totalRoutes: completedRoutes.length,
      totalDistance,
      totalMinutes,
      averageRisk,
      savedRoutes,
    };
  }, [completedRoutes]);

  const recentRoutes = completedRoutes.slice(0, 5);

  return (
    <main className="dashboard-page">
      <section className="dashboard-hero">
        <div>
          <span className="dashboard-eyebrow">
            ROAMORA DASHBOARD
          </span>

          <h1>
            Your journeys,
            <br />
            all in one place.
          </h1>

          <p>
            Review your recent routes, saved journeys,
            and travel insights.
          </p>
        </div>

        <Link
          to="/planner"
          className="dashboard-plan-button"
        >
          <Plus size={18} />
          Plan a journey
        </Link>
      </section>

      <section className="dashboard-stats">
        <article className="dashboard-stat-card">
          <div className="dashboard-stat-icon">
            <Route size={19} />
          </div>

          <span>ROUTES PLANNED</span>
          <strong>{stats.totalRoutes}</strong>
          <small>Your route history</small>
        </article>

        <article className="dashboard-stat-card">
          <div className="dashboard-stat-icon">
            <Map size={19} />
          </div>

          <span>DISTANCE PLANNED</span>
          <strong>
            {stats.totalDistance.toFixed(1)}
            <small className="dashboard-stat-unit">
              km
            </small>
          </strong>
          <small>Across your journeys</small>
        </article>

        <article className="dashboard-stat-card">
          <div className="dashboard-stat-icon">
            <Clock3 size={19} />
          </div>

          <span>TRAVEL TIME</span>
          <strong>
            {formatDuration(stats.totalMinutes)}
          </strong>
          <small>Estimated journey time</small>
        </article>

        <article className="dashboard-stat-card">
          <div className="dashboard-stat-icon">
            <ShieldCheck size={19} />
          </div>

          <span>AVERAGE RISK</span>
          <strong>
            {stats.averageRisk !== null
              ? `${stats.averageRisk}/100`
              : "—"}
          </strong>
          <small>Across analyzed routes</small>
        </article>
      </section>

      {error && (
        <div className="dashboard-error" role="alert">
          <strong>Journey data is temporarily unavailable.</strong>
          <span>{error}</span>
        </div>
      )}

      <section className="dashboard-content">
        <div className="dashboard-main-column">
          <div className="dashboard-section-header">
            <div>
              <span>RECENT JOURNEYS</span>
              <h2>Latest routes</h2>
            </div>

            <Link to="/history">
              View all
              <ArrowRight size={15} />
            </Link>
          </div>

          {loading ? (
            <div className="dashboard-empty">
              <div className="dashboard-empty-icon">
                <Route size={23} />
              </div>

              <h3>Loading journeys...</h3>

              <p>
                Fetching your route history.
              </p>
            </div>
          ) : recentRoutes.length > 0 ? (
            <div className="dashboard-route-list">
              {recentRoutes.map((route, index) => (
                <article
                  className="dashboard-route-card"
                  key={route.id || index}
                >
                  <div className="dashboard-route-number">
                    {String(index + 1).padStart(2, "0")}
                  </div>

                  <div className="dashboard-route-details">
                    <div className="dashboard-route-path">
                      <strong>
                        {route.origin}
                      </strong>

                      <ArrowRight size={15} />

                      <strong>
                        {route.destination}
                      </strong>
                    </div>

                    <div className="dashboard-route-meta">
                      <span>
                        {route.transportMode ||
                          "CAR"}
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
                        {formatDuration(
                          route.durationMinutes
                        )}
                      </span>

                      <span>
                        {formatDate(
                          route.createdAt
                        )}
                      </span>
                    </div>
                  </div>

                  <div
                    className={`dashboard-risk ${
                      String(
                        route.status ||
                          route.riskLevel ||
                          ""
                      ).toLowerCase()
                    }`}
                  >
                    <ShieldCheck size={15} />

                    <span>
                      {route.status ||
                        route.riskLevel ||
                        "Unknown"}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="dashboard-empty">
              <div className="dashboard-empty-icon">
                <Map size={23} />
              </div>

              <h3>No journeys yet</h3>

              <p>
                Plan your first weather-aware journey
                to see it here.
              </p>

              <Link
                to="/planner"
                className="dashboard-empty-button"
              >
                Plan a journey
                <ArrowRight size={16} />
              </Link>
            </div>
          )}
        </div>

        <aside className="dashboard-sidebar">
          <div className="dashboard-sidebar-card">
            <div className="dashboard-sidebar-icon">
              <Bookmark size={19} />
            </div>

            <span>SAVED ROUTES</span>

            <strong>{stats.savedRoutes}</strong>

            <p>
              Routes you've bookmarked for later.
            </p>

            <Link to="/history">
              Open saved routes
              <ArrowRight size={15} />
            </Link>
          </div>

          <div className="dashboard-insight-card">
            <TrendingUp size={19} />

            <div>
              <span>ROAMORA INSIGHT</span>

              <p>
                Compare multiple alternatives before
                choosing a route. Weather and risk
                conditions can change your preferred
                journey.
              </p>
            </div>
          </div>
        </aside>
      </section>
    </main>
  );
}

export default DashboardPage;