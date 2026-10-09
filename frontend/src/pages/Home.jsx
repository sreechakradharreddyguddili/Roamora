import {
  ArrowDownRight,
  ArrowRight,
  ArrowUp,
  CloudRain,
  CloudSun,
  Compass,
  MapPinned,
  Navigation,
  ShieldCheck,
  Sparkles,
  Sun,
  Wind,
  AlertTriangle,
  LocateFixed,
} from "lucide-react";

import { Link } from "react-router-dom";

import {
  useReveal,
  useScrollMotion,
} from "../utils/scrollMotion";

import "./Home.css";

export default function Home() {
  useReveal();
  useScrollMotion();

  return (
    <main className="roamora-home">

      {/* =====================================================
          HERO
      ===================================================== */}

      <section className="hero">

        <div className="hero-content">

          <div className="hero-eyebrow reveal">
            <span />
            INTELLIGENCE FOR EVERY JOURNEY
          </div>

          <h1 className="reveal reveal-delay-1">
            Don't just
            <br />

            <span>find a route.</span>

            <br />

            <strong>
              understand it.
            </strong>
          </h1>

          <p className="hero-description reveal reveal-delay-2">
            Roamora connects routes, weather and travel
            risk into one clear planning experience—so
            you can understand the journey before you begin.
          </p>

          <div className="hero-buttons reveal reveal-delay-3">

            <Link
              to="/planner"
              className="primary-button"
            >
              Plan your journey
              <ArrowRight size={17} />
            </Link>

            <Link
              to="/explore"
              className="outline-button"
            >
              Explore Roamora
              <ArrowDownRight size={16} />
            </Link>

          </div>

        </div>


        {/* =================================================
            HERO VISUAL
        ================================================= */}

        <div
          className="hero-visual"
          data-motion
          data-x="18"
          data-y="-15"
          data-rotate="2"
          data-scale="0.04"
        >

          <div className="hero-glow" />

          <div
            className="hero-orbit orbit-large"
            data-motion
            data-x="-15"
            data-y="10"
            data-rotate="4"
          />

          <div
            className="hero-orbit orbit-small"
            data-motion
            data-x="15"
            data-y="-15"
            data-rotate="-5"
          />


          <div className="route-preview">

            <div className="preview-header">

              <div>

                <small>
                  YOUR JOURNEY
                </small>

                <strong>
                  Hyderabad → Bengaluru
                </strong>

              </div>

              <span className="live">
                <i />
                LIVE
              </span>

            </div>


            <div className="route-map">

              <div className="map-grid" />

              <span className="map-road road-a" />
              <span className="map-road road-b" />
              <span className="map-road road-c" />
              <span className="map-road road-d" />


              <svg
                className="route-svg"
                viewBox="0 0 700 400"
              >

                <path
                  d="
                    M80 310
                    C180 270 160 100 330 175
                    S510 320 630 70
                  "
                />

              </svg>


              <div className="map-pin start-pin">
                H
              </div>

              <div className="map-pin end-pin">
                B
              </div>


              <div
                className="map-label start-label"
                data-motion
                data-x="-8"
                data-y="5"
              >

                <strong>
                  Hyderabad
                </strong>

                <small>
                  28°C · Clear
                </small>

              </div>


              <div
                className="map-label end-label"
                data-motion
                data-x="8"
                data-y="-5"
              >

                <strong>
                  Bengaluru
                </strong>

                <small>
                  24°C · Light rain
                </small>

              </div>


              <div
                className="weather-bubble bubble-one"
                data-motion
                data-x="-12"
                data-y="-15"
                data-rotate="-2"
              >

                <Sun size={17} />

                <div>

                  <small>
                    NOW
                  </small>

                  <strong>
                    28°
                  </strong>

                </div>

              </div>


              <div
                className="weather-bubble bubble-two"
                data-motion
                data-x="12"
                data-y="12"
                data-rotate="2"
              >

                <CloudRain size={17} />

                <div>

                  <small>
                    DESTINATION
                  </small>

                  <strong>
                    24°
                  </strong>

                </div>

              </div>

            </div>


            <div className="preview-footer">

              <div>

                <small>
                  FASTEST
                </small>

                <strong>
                  7h 12m
                </strong>

              </div>

              <div>

                <small>
                  DISTANCE
                </small>

                <strong>
                  570 km
                </strong>

              </div>

              <span className="risk-badge">
                <ShieldCheck size={15} />
                Low risk
              </span>

            </div>

          </div>

        </div>

      </section>


      {/* =====================================================
          INTRO
      ===================================================== */}

      <section className="intro">

        <span className="section-index">
          01
        </span>

        <p>

          <strong>
            The journey changes.
          </strong>{" "}

          Weather changes. Roads change.
          Conditions change. Your understanding
          of the journey should change with them.

        </p>


        {/* JOURNEY INTELLIGENCE NAVIGATION */}

        <Link
          to="/journey-intelligence"
          className="intro-arrow"
          aria-label="Go to Roamora Journey Intelligence"
        >
          <ArrowDownRight size={22} />
        </Link>

      </section>


      {/* =====================================================
          EXPERIENCE
      ===================================================== */}

      <section
        className="experience"
        id="features"
      >

        <div className="section-heading reveal">

          <div>

            <span>
              02 / EXPERIENCE
            </span>

            <h2>
              From data
              <br />
              to <em>decision.</em>
            </h2>

          </div>

          <p>
            Four layers of intelligence.
            One clearer journey.
          </p>

        </div>


        <div className="experience-stage">

          {/* ROUTE */}

          <div
            className="experience-card card-route reveal"
            data-motion
            data-x="-10"
            data-y="8"
            data-rotate="-1"
            data-scale="0.02"
          >

            <div className="experience-icon">
              <Compass />
            </div>

            <span>
              01
            </span>

            <h3>
              Route
            </h3>

            <p>
              Where are you going?
            </p>

          </div>


          {/* WEATHER */}

          <div
            className="experience-card card-weather reveal reveal-delay-1"
            data-motion
            data-x="-4"
            data-y="-8"
            data-rotate="1"
            data-scale="0.02"
          >

            <div className="experience-icon">
              <CloudSun />
            </div>

            <span>
              02
            </span>

            <h3>
              Weather
            </h3>

            <p>
              What will you encounter?
            </p>

          </div>


          {/* RISK */}

          <div
            className="experience-card card-risk reveal reveal-delay-2"
            data-motion
            data-x="4"
            data-y="8"
            data-rotate="-1"
            data-scale="0.02"
          >

            <div className="experience-icon">
              <ShieldCheck />
            </div>

            <span>
              03
            </span>

            <h3>
              Risk
            </h3>

            <p>
              What should you consider?
            </p>

          </div>


          {/* DECISION */}

          <div
            className="experience-card card-decision reveal reveal-delay-3"
            data-motion
            data-x="10"
            data-y="-8"
            data-rotate="1"
            data-scale="0.02"
          >

            <div className="experience-icon">
              <Navigation />
            </div>

            <span>
              04
            </span>

            <h3>
              Decision
            </h3>

            <p>
              Which route makes sense?
            </p>

          </div>

        </div>

      </section>


      {/* =====================================================
          ROUTE INTELLIGENCE
      ===================================================== */}

      <section className="route-section">

        <div
          className="route-intelligence-visual"
          data-motion
          data-x="-15"
          data-y="12"
          data-rotate="-1"
          data-scale="0.02"
        >

          <div className="route-terrain" />

          <div className="terrain-lines terrain-one" />
          <div className="terrain-lines terrain-two" />
          <div className="terrain-lines terrain-three" />


          <svg
            className="intelligence-route-svg"
            viewBox="0 0 700 520"
            preserveAspectRatio="none"
          >

            <path
              className="route-shadow"
              d="
                M70 410
                C145 350 130 250 245 270
                C340 290 310 150 410 175
                C500 200 485 110 625 80
              "
            />

            <path
              className="main-route"
              d="
                M70 410
                C145 350 130 250 245 270
                C340 290 310 150 410 175
                C500 200 485 110 625 80
              "
            />

            <path
              className="alternate-route"
              d="
                M70 410
                C180 390 180 330 275 350
                C370 370 430 280 480 260
                C535 235 550 170 625 80
              "
            />

          </svg>


          <div className="route-point route-start">
            <span />
          </div>

          <div className="route-point route-destination">
            <span />
          </div>


          <div
            className="route-weather-card weather-clear"
            data-motion
            data-x="-8"
            data-y="-6"
            data-rotate="-1"
          >

            <Sun size={18} />

            <div>

              <small>
                HYDERABAD
              </small>

              <strong>
                28°C
              </strong>

              <span>
                Clear skies
              </span>

            </div>

          </div>


          <div
            className="route-weather-card weather-rain"
            data-motion
            data-x="8"
            data-y="8"
            data-rotate="1"
          >

            <CloudRain size={18} />

            <div>

              <small>
                DESTINATION
              </small>

              <strong>
                24°C
              </strong>

              <span>
                Light rain
              </span>

            </div>

          </div>


          <div className="route-data-card elevation-card">

            <MapPinned size={17} />

            <div>

              <small>
                ELEVATION
              </small>

              <strong>
                +1,240 m
              </strong>

            </div>

          </div>


          <div className="route-options-card">

            <div className="route-option active">

              <span className="option-line blue-line" />

              <div>

                <small>
                  FASTEST
                </small>

                <strong>
                  7h 12m
                </strong>

              </div>

              <b>
                570 km
              </b>

            </div>


            <div className="route-option">

              <span className="option-line green-line" />

              <div>

                <small>
                  SCENIC
                </small>

                <strong>
                  7h 48m
                </strong>

              </div>

              <b>
                612 km
              </b>

            </div>


            <div className="route-option">

              <span className="option-line gray-line" />

              <div>

                <small>
                  LOW RISK
                </small>

                <strong>
                  8h 05m
                </strong>

              </div>

              <b>
                630 km
              </b>

            </div>

          </div>

        </div>


        <div
          className="route-copy reveal"
          data-motion
          data-x="15"
          data-y="-10"
          data-rotate="1"
        >

          <span>
            03 / ROUTE INTELLIGENCE
          </span>

          <h2>
            The best route
            <br />
            isn't always
            <br />
            the <em>shortest one.</em>
          </h2>

          <p>
            Compare alternative routes by more than distance.
            Roamora considers time, weather, elevation and
            travel conditions before you decide.
          </p>


          <div className="route-insight-list">

            <div>

              <span className="insight-dot blue-dot" />

              <p>
                <strong>
                  Fastest
                </strong>
                Get there sooner.
              </p>

            </div>


            <div>

              <span className="insight-dot green-dot" />

              <p>
                <strong>
                  Scenic
                </strong>
                Take the road worth remembering.
              </p>

            </div>


            <div>

              <span className="insight-dot gray-dot" />

              <p>
                <strong>
                  Lower risk
                </strong>
                Trade a little time for calmer conditions.
              </p>

            </div>

          </div>


          <Link
            to="/explore"
            className="primary-button"
          >
            Explore routes
            <ArrowRight size={17} />
          </Link>

        </div>

      </section>


      {/* =====================================================
          INTELLIGENCE
      ===================================================== */}

      <section
        className="intelligence"
        id="how-it-works"
      >

        <div className="intelligence-heading reveal">

          <span>
            04 / INTELLIGENCE
          </span>

          <h2>
            Every decision
            <br />
            starts with
            <em> context.</em>
          </h2>

        </div>


        <div className="intelligence-grid">

          <div
            className="intel-item"
            data-motion
            data-x="-10"
            data-y="8"
            data-rotate="-1"
          >

            <CloudSun />

            <strong>
              Weather
            </strong>

            <span>
              Conditions along the journey.
            </span>

          </div>


          <div
            className="intel-item"
            data-motion
            data-x="-4"
            data-y="-8"
            data-rotate="1"
          >

            <MapPinned />

            <strong>
              Route
            </strong>

            <span>
              Alternative paths and trade-offs.
            </span>

          </div>


          <div
            className="intel-item"
            data-motion
            data-x="4"
            data-y="8"
            data-rotate="-1"
          >

            <ShieldCheck />

            <strong>
              Risk
            </strong>

            <span>
              A clear signal from complex conditions.
            </span>

          </div>


          <div
            className="intel-item final-intel"
            data-motion
            data-x="10"
            data-y="-8"
            data-rotate="1"
          >

            <Navigation />

            <strong>
              Decision
            </strong>

            <span>
              Choose with better context.
            </span>

          </div>

        </div>

      </section>


      {/* =====================================================
          ABOUT / WHY ROAMORA
      ===================================================== */}

      <section
        className="about"
        id="about"
      >

        <div
          className="about-intelligence-visual reveal"
          data-motion
          data-x="-15"
          data-y="10"
          data-rotate="-1"
          data-scale="0.02"
        >

          <div className="about-map" />

          <div className="about-contour contour-one" />
          <div className="about-contour contour-two" />
          <div className="about-contour contour-three" />


          <div className="radar-system">

            <div className="radar-ring ring-one" />
            <div className="radar-ring ring-two" />
            <div className="radar-ring ring-three" />

            <div className="radar-center">
              <Navigation size={19} />
            </div>

          </div>


          <div
            className="about-data-card weather-status"
            data-motion
            data-x="-8"
            data-y="-6"
            data-rotate="-1"
          >

            <CloudSun size={19} />

            <div>

              <small>
                WEATHER
              </small>

              <strong>
                28°C
              </strong>

              <span>
                Partly cloudy
              </span>

            </div>

            <ArrowRight size={14} />

          </div>


          <div
            className="about-data-card wind-status"
            data-motion
            data-x="8"
            data-y="6"
            data-rotate="1"
          >

            <Wind size={19} />

            <div>

              <small>
                WIND
              </small>

              <strong>
                12 km/h
              </strong>

              <span>
                North east
              </span>

            </div>

            <ArrowRight size={14} />

          </div>


          <div
            className="about-data-card risk-status"
            data-motion
            data-x="6"
            data-y="-6"
            data-rotate="-1"
          >

            <AlertTriangle size={19} />

            <div>

              <small>
                TRAVEL RISK
              </small>

              <strong>
                Low
              </strong>

              <span>
                Conditions stable
              </span>

            </div>

            <ArrowRight size={14} />

          </div>


          <div className="location-card">

            <LocateFixed size={18} />

            <div>

              <small>
                CURRENT LOCATION
              </small>

              <strong>
                17.3850° N, 78.4867° E
              </strong>

            </div>

          </div>


          <div className="map-coordinate coordinate-top">
            17.3850° N
          </div>

          <div className="map-coordinate coordinate-bottom">
            78.4867° E
          </div>

        </div>


        <div
          className="about-copy reveal"
          data-motion
          data-x="15"
          data-y="-8"
          data-rotate="1"
        >

          <span>
            05 / WHY ROAMORA
          </span>

          <h2>
            A better route
            <br />
            starts with
            <br />
            <em>better information.</em>
          </h2>

          <p>
            Roamora brings route planning, weather intelligence
            and travel risk into one focused experience.
            Instead of simply showing you a route, it helps
            you understand the journey ahead.
          </p>


          <div className="about-feature-list">

            <div className="about-feature">

              <div className="feature-icon">
                <MapPinned size={17} />
              </div>

              <div>

                <strong>
                  Route intelligence
                </strong>

                <span>
                  Understand alternative paths.
                </span>

              </div>

            </div>


            <div className="about-feature">

              <div className="feature-icon">
                <CloudSun size={17} />
              </div>

              <div>

                <strong>
                  Weather context
                </strong>

                <span>
                  Know what conditions are ahead.
                </span>

              </div>

            </div>


            <div className="about-feature">

              <div className="feature-icon">
                <ShieldCheck size={17} />
              </div>

              <div>

                <strong>
                  Travel risk
                </strong>

                <span>
                  See the bigger picture before leaving.
                </span>

              </div>

            </div>

          </div>


          <Link
            to="/planner"
            className="primary-button"
          >
            Start planning
            <ArrowRight size={17} />
          </Link>

        </div>

      </section>


      {/* =====================================================
          FINAL CTA
      ===================================================== */}

      <section className="final-cta">

        <div
          className="final-orbit"
          data-motion
          data-x="10"
          data-y="-15"
          data-rotate="5"
          data-scale="0.04"
        />

        <div
          className="final-content reveal"
          data-motion
          data-y="15"
          data-scale="0.03"
        >

          <Sparkles />

          <span>
            YOUR NEXT JOURNEY
          </span>

          <h2>
            Go with
            <br />
            <em>better context.</em>
          </h2>

          <p>
            Build your route. Understand the conditions.
            Make the journey yours.
          </p>

          <Link
            to="/planner"
            className="primary-button"
          >
            Plan your route
            <ArrowRight size={17} />
          </Link>

        </div>

      </section>


      {/* =====================================================
          BACK TO TOP
      ===================================================== */}

      <button
        type="button"
        className="back-to-top"
        onClick={() =>
          window.scrollTo({
            top: 0,
            behavior: "smooth",
          })
        }
        aria-label="Back to top"
        title="Back to top"
      >
        <ArrowUp size={20} />
      </button>


      {/* =====================================================
          FOOTER
      ===================================================== */}

      <footer className="footer">

        <Link
          to="/"
          className="brand"
        >

          <span className="brand-mark">
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