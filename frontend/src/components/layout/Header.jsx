import {
  Menu,
  X,
  LogOut,
  LayoutDashboard,
  Compass,
  ArrowUpRight,
} from "lucide-react";

import { useState } from "react";

import {
  Link,
  useLocation,
  useNavigate,
} from "react-router-dom";

import { logout } from "../../api/authApi";
import { storage } from "../../utils/storage";

import "./Header.css";

function Header({ session, onLogout }) {
  const [open, setOpen] = useState(false);

  const location = useLocation();
  const navigate = useNavigate();

  const close = () => {
    setOpen(false);
  };

  const go = (path) => {
    close();
    navigate(path);
  };

  const handleLogout = async () => {
    const currentSession = storage.getSession();

    try {
      if (currentSession?.refreshToken) {
        await logout(currentSession.refreshToken);
      }
    } catch (error) {
      console.error("Logout request failed:", error);
    } finally {
      storage.clearSession();

      if (onLogout) {
        onLogout();
      }

      navigate("/login", {
        replace: true,
      });

      close();
    }
  };

  const isFeatures = location.pathname === "/features";
  const isHowItWorks = location.pathname === "/how-it-works";
  const isAbout = location.pathname === "/about";
  const isExplore = location.pathname.startsWith("/explore");
  const isStays = location.pathname.startsWith("/stays");
  const isTransport = location.pathname.startsWith("/transport");
  const isPlanner = location.pathname.startsWith("/planner");
  const isProfile = location.pathname.startsWith("/profile");

  return (
    <header className="roamora-header">

      <div className="roamora-header-inner">

        {/* =================================================
            BRAND
        ================================================= */}

        <Link
          to="/"
          className="roamora-brand"
          onClick={close}
          aria-label="Roamora Home"
        >
          <span className="roamora-brand-mark">
            <Compass
              size={18}
              strokeWidth={1.8}
            />
          </span>

          <span className="roamora-brand-name">
            Roamora
          </span>
        </Link>


        {/* =================================================
            NAVIGATION
        ================================================= */}

        <nav
          className={`roamora-nav ${
            open ? "is-open" : ""
          }`}
        >

          <Link
            to="/features"
            onClick={close}
            className={isFeatures ? "active" : ""}
          >
            Features
          </Link>


          <Link
            to="/how-it-works"
            onClick={close}
            className={isHowItWorks ? "active" : ""}
          >
            How it works
          </Link>


          <Link
            to="/about"
            onClick={close}
            className={isAbout ? "active" : ""}
          >
            About
          </Link>


          <Link
            to="/explore"
            onClick={close}
            className={isExplore ? "active" : ""}
          >
            Explore
          </Link>

          <Link
            to="/stays"
            onClick={close}
            className={isStays ? "active" : ""}
          >
            Stays
          </Link>

          <Link
            to="/transport"
            onClick={close}
            className={isTransport ? "active" : ""}
          >
            Transport
          </Link>


          {session && (
            <>
              <Link
                to="/planner"
                onClick={close}
                className={isPlanner ? "active" : ""}
              >
                Planner
              </Link>
              <Link
                to="/profile"
                onClick={close}
                className={isProfile ? "active" : ""}
              >
                Profile
              </Link>
            </>
          )}

        </nav>


        {/* =================================================
            ACTIONS
        ================================================= */}

        <div className="roamora-header-actions">

          {session ? (
            <>

              <button
                type="button"
                className="header-dashboard"
                onClick={() => go("/dashboard")}
              >
                <LayoutDashboard size={16} />

                <span>
                  Dashboard
                </span>
              </button>


              <button
                type="button"
                className="header-logout"
                onClick={handleLogout}
              >
                <LogOut size={15} />

                <span>
                  Logout
                </span>
              </button>

            </>
          ) : (
            <>

              <button
                type="button"
                className="header-signin"
                onClick={() => go("/login")}
              >
                Sign in
              </button>


              <button
                type="button"
                className="header-cta"
                onClick={() => go("/signup")}
              >
                <span>
                  Get started
                </span>

                <ArrowUpRight size={15} />
              </button>

            </>
          )}


          {/* MOBILE MENU */}

          <button
            type="button"
            className="header-menu"
            onClick={() => setOpen((value) => !value)}
            aria-label={
              open
                ? "Close navigation"
                : "Open navigation"
            }
            aria-expanded={open}
          >
            {open ? (
              <X size={20} />
            ) : (
              <Menu size={20} />
            )}
          </button>

        </div>

      </div>


      {/* MOBILE BACKDROP */}

      {open && (
        <button
          type="button"
          className="header-mobile-backdrop"
          onClick={close}
          aria-label="Close navigation"
        />
      )}

    </header>
  );
}

export default Header;