import { ArrowUpRight, Compass, MoveUpRight } from "lucide-react";
import { Link } from "react-router-dom";
import "./Footer.css";

export default function Footer() {
  return (
    <footer className="roamora-footer">

      {/* TOP EXPERIENCE */}
      <div className="footer-journey">

        <div className="footer-journey-grid" />

        <div className="footer-journey-content">
          <span className="footer-kicker">
            ROAMORA / JOURNEY INTELLIGENCE
          </span>

          <h2>
            Plan the journey
            <br />
            <span>before you take it.</span>
          </h2>

          <p>
            Routes change. Weather changes. Conditions change.
            Roamora brings everything together so you can understand
            what lies ahead.
          </p>

          <div className="footer-actions">
            <Link to="/planner" className="footer-main-button">
              Plan your journey
              <ArrowUpRight size={18} />
            </Link>

            <Link to="/explore" className="footer-explore-link">
              Explore Roamora
              <MoveUpRight size={16} />
            </Link>
          </div>
        </div>

        {/* MOVING ROUTE VISUAL */}
        <div className="footer-route-visual">

          <div className="footer-orbit orbit-one" />
          <div className="footer-orbit orbit-two" />
          <div className="footer-orbit orbit-three" />

          <div className="footer-route-line">
            <span className="route-dot dot-one" />
            <span className="route-dot dot-two" />
            <span className="route-dot dot-three" />
            <span className="route-traveller" />
          </div>

          <div className="footer-location location-one">
            <span>ORIGIN</span>
          </div>

          <div className="footer-location location-two">
            <span>DESTINATION</span>
          </div>

          <div className="footer-compass">
            <Compass size={27} />
          </div>

        </div>
      </div>

      {/* NAVIGATION */}
      <div className="footer-main">

        <div className="footer-brand-area">

          <Link to="/" className="footer-brand">
            <span className="footer-brand-mark">◈</span>

            <div>
              <strong>Roamora</strong>
              <small>Journey intelligence</small>
            </div>
          </Link>

          <p>
            Weather-aware route intelligence for people who
            want to understand their journey before they travel.
          </p>

          <div className="footer-system-status">
            <i />
            <span>Systems ready</span>
            <b>LIVE</b>
          </div>

        </div>

        <div className="footer-column">
          <h3>Product</h3>

          <Link to="/planner">
            Planner
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/dashboard">
            Dashboard
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/history">
            History
            <ArrowUpRight size={13} />
          </Link>
        </div>

        <div className="footer-column">
          <h3>Explore</h3>

          <Link to="/explore">
            Explore Roamora
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/#features">
            Features
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/#how-it-works">
            How it works
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/#about">
            About
            <ArrowUpRight size={13} />
          </Link>
        </div>

        <div className="footer-column">
          <h3>Account</h3>

          <Link to="/login">
            Sign in
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/signup">
            Create account
            <ArrowUpRight size={13} />
          </Link>

          <Link to="/profile">
            Preferences
            <ArrowUpRight size={13} />
          </Link>
        </div>

      </div>

      {/* BOTTOM */}
      <div className="footer-bottom">

        <span>
          © {new Date().getFullYear()} Roamora
        </span>

        <div className="footer-bottom-center">
          <span>ROUTES</span>
          <i />
          <span>WEATHER</span>
          <i />
          <span>RISK</span>
          <i />
          <span>DECISIONS</span>
        </div>

        <span className="footer-tagline">
          Plan better. Travel safer.
        </span>

      </div>

    </footer>
  );
}