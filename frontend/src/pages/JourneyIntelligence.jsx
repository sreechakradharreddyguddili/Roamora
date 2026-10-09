import {
  ArrowRight,
  CloudSun,
  Compass,
  Navigation,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

import { Link } from "react-router-dom";

import "./JourneyIntelligence.css";

export default function JourneyIntelligence() {
  return (
    <main className="journey-page">

      {/* =====================================================
          HERO
      ===================================================== */}

      <section className="journey-hero">

        <div className="journey-hero-copy">

          <span className="journey-eyebrow">
            ROAMORA / JOURNEY INTELLIGENCE
          </span>

          <h1>
            Four layers.
            <br />
            <em>One clearer journey.</em>
          </h1>

          <p>
            Roamora combines route, weather, risk and decision
            context into one intelligence layer—so you can
            understand the journey before you begin.
          </p>

          <div className="journey-hero-actions">

            <Link
              to="/planner"
              className="journey-primary-button"
            >
              Plan your journey
              <ArrowRight size={17} />
            </Link>

            <a
              href="#layers"
              className="journey-text-link"
            >
              Explore the intelligence
              <ArrowRight size={16} />
            </a>

          </div>

        </div>


        <div className="journey-hero-visual">

          <div className="journey-orbit journey-orbit-one" />
          <div className="journey-orbit journey-orbit-two" />

          <div className="journey-center-card">

            <div className="journey-center-icon">
              <Sparkles size={21} />
            </div>

            <small>
              JOURNEY CONTEXT
            </small>

            <strong>
              Hyderabad → Bengaluru
            </strong>

            <span>
              Route understood.
              Conditions considered.
            </span>

            <div className="journey-mini-status">
              <i />
              Intelligence active
            </div>

          </div>

        </div>

      </section>


      {/* =====================================================
          INTELLIGENCE LAYERS
      ===================================================== */}

      <section
        className="journey-layers"
        id="layers"
      >

        <div className="journey-section-heading">

          <div>
            <span>
              01 / INTELLIGENCE LAYERS
            </span>

            <h2>
              From raw conditions
              <br />
              to <em>better context.</em>
            </h2>
          </div>

          <p>
            Each layer answers a different question.
            Together they create a clearer picture of
            the journey ahead.
          </p>

        </div>


        <div className="journey-layer-grid">

          {/* ROUTE */}

          <article tabIndex={0} className="journey-layer-card route-layer card-interaction-route">

            <div className="journey-layer-icon">
              <Compass size={23} />
            </div>

            <span className="journey-card-number">
              01
            </span>

            <h3>
              Route
            </h3>

            <p>
              Where are you going?
              Compare paths using distance, time
              and journey conditions.
            </p>

            <div className="journey-card-line" />

            <small>
              ROUTE INTELLIGENCE
            </small>

          </article>


          {/* WEATHER */}

          <article tabIndex={0} className="journey-layer-card weather-layer card-interaction-weather">

            <div className="journey-layer-icon">
              <CloudSun size={23} />
            </div>

            <span className="journey-card-number">
              02
            </span>

            <h3>
              Weather
            </h3>

            <p>
              What will you encounter?
              Understand conditions across the
              route instead of only at the destination.
            </p>

            <div className="journey-card-line" />

            <small>
              WEATHER CONTEXT
            </small>

          </article>


          {/* RISK */}

          <article tabIndex={0} className="journey-layer-card risk-layer card-interaction-risk">

            <div className="journey-layer-icon">
              <ShieldCheck size={23} />
            </div>

            <span className="journey-card-number">
              03
            </span>

            <h3>
              Risk
            </h3>

            <p>
              What should you consider?
              Turn changing conditions into a
              clearer travel signal.
            </p>

            <div className="journey-card-line" />

            <small>
              TRAVEL RISK
            </small>

          </article>


          {/* DECISION */}

          <article tabIndex={0} className="journey-layer-card decision-layer card-interaction-decision">

            <div className="journey-layer-icon">
              <Navigation size={23} />
            </div>

            <span className="journey-card-number">
              04
            </span>

            <h3>
              Decision
            </h3>

            <p>
              Which route makes sense?
              Bring every relevant signal together
              before choosing your path.
            </p>

            <div className="journey-card-line" />

            <small>
              DECISION CONTEXT
            </small>

          </article>

        </div>

      </section>


      {/* =====================================================
          FLOW
      ===================================================== */}

      <section className="journey-flow">

        <span>
          02 / HOW IT CONNECTS
        </span>

        <h2>
          Route.
          <em>Weather.</em>
          Risk.
          <br />
          Then a clearer decision.
        </h2>

        <div className="journey-flow-track">

          <div className="journey-flow-step flow-route">
            <div className="journey-flow-node">
              <b>01</b>
            </div>

            <span>Route</span>

            <small>
              Find the path
            </small>
          </div>


          <div className="journey-flow-connector">
            <span />
            <i />
          </div>


          <div className="journey-flow-step flow-weather">
            <div className="journey-flow-node">
              <b>02</b>
            </div>

            <span>Weather</span>

            <small>
              Read conditions
            </small>
          </div>


          <div className="journey-flow-connector">
            <span />
            <i />
          </div>


          <div className="journey-flow-step flow-risk">
            <div className="journey-flow-node">
              <b>03</b>
            </div>

            <span>Risk</span>

            <small>
              Understand trade-offs
            </small>
          </div>


          <div className="journey-flow-connector">
            <span />
            <i />
          </div>


          <div className="journey-flow-step flow-decision">
            <div className="journey-flow-node">
              <b>04</b>
            </div>

            <span>Decision</span>

            <small>
              Choose with context
            </small>
          </div>

        </div>

      </section>


      {/* =====================================================
          CTA
      ===================================================== */}

      <section className="journey-cta">

        <div>

          <span>
            READY FOR THE NEXT JOURNEY?
          </span>

          <h2>
            Plan with
            <br />
            <em>better context.</em>
          </h2>

          <p>
            Build your route, understand the conditions
            and start your journey with a clearer picture.
          </p>

          <Link
            to="/planner"
            className="journey-primary-button"
          >
            Open planner
            <ArrowRight size={17} />
          </Link>

        </div>

      </section>


      {/* =====================================================
          FOOTER
      ===================================================== */}

      <footer className="journey-footer">

        <Link to="/" className="journey-brand">
          <span className="journey-brand-mark">
            R
          </span>

          roamora
        </Link>

        <span>
          Route intelligence for better journeys.
        </span>

        <span>
          © {new Date().getFullYear()} Roamora
        </span>

      </footer>

    </main>
  );
}
