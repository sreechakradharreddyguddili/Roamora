import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { Component, useEffect, useState } from "react";

import Header from "./components/layout/Header";
import Footer from "./components/layout/Footer";
import AntiGravityPage from "./components/layout/AntiGravityPage";
import RoamoraCopilot from "./components/intelligence/RoamoraCopilot";

import Home from "./pages/Home";
import JourneyIntelligence from "./pages/JourneyIntelligence";
import JourneyWorkspacePage from "./pages/JourneyWorkspacePage";
import FeaturesPage from "./pages/FeaturesPage";
import HowItWorksPage from "./pages/HowItWorksPage";
import AboutPage from "./pages/AboutPage";
import Explore from "./pages/Explore";
import StaysPage from "./pages/StaysPage";
import PlannerPage from "./pages/PlannerPage";
import DashboardPage from "./pages/DashboardPage";
import HistoryPage from "./pages/HistoryPage";
import ProfilePage from "./pages/ProfilePage";
import { LoginPage, SignupPage } from "./pages/AuthPages";
import OAuth2CallbackPage from "./pages/OAuth2CallbackPage";
import NotFoundPage from "./pages/NotFoundPage";

import { storage } from "./utils/storage";

import "./App.css";

function ProtectedRoute({ session, children }) {
  if (!session?.token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error) {
    console.error("Roamora UI error:", error);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <main className="roamora-crash-page">
        <div className="roamora-crash-card">
          <span>ROAMORA</span>
          <h1>Something interrupted the journey.</h1>
          <p>
            Roamora hit an unexpected interface error. Your saved session is kept intact.
            Reload the page and continue where you left off.
          </p>
          <button type="button" onClick={this.handleReload}>
            Reload Roamora
          </button>
        </div>
      </main>
    );
  }
}

function ScrollManager() {
  const location = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname, location.search]);

  return null;
}

function Page({ children, className = "" }) {
  return (
    <AntiGravityPage className={className}>
      {children}
    </AntiGravityPage>
  );
}

export default function App() {
  const [session, setSession] = useState(() => storage.getSession());

  useEffect(() => {
    const handler = () => setSession(storage.getSession());

    window.addEventListener("roamora:storage", handler);
    return () => window.removeEventListener("roamora:storage", handler);
  }, []);

  const handleAuthenticated = () => {
    setSession(storage.getSession());
  };

  const handleLogout = () => {
    storage.clearSession();
    setSession(null);
  };

  return (
    <AppErrorBoundary>
      <BrowserRouter>
        <ScrollManager />

        <div className="roamora-app">
          <Header session={session} onLogout={handleLogout} />

          <Routes>
            <Route path="/" element={<Page><Home /></Page>} />
            <Route path="/features" element={<Page><FeaturesPage /></Page>} />
            <Route path="/how-it-works" element={<Page><HowItWorksPage /></Page>} />
            <Route path="/about" element={<Page><AboutPage /></Page>} />
            <Route path="/explore" element={<Page><Explore /></Page>} />
            <Route path="/stays" element={<Page><StaysPage /></Page>} />
            <Route path="/journey-intelligence" element={<Page><JourneyIntelligence /></Page>} />
            <Route path="/journey-workspace" element={<Page><JourneyWorkspacePage /></Page>} />

            <Route
              path="/login"
              element={
                session?.token ? (
                  <Navigate to="/dashboard" replace />
                ) : (
                  <Page className="auth-page-wrapper">
                    <LoginPage onAuthenticated={handleAuthenticated} />
                  </Page>
                )
              }
            />

            <Route
              path="/signup"
              element={
                session?.token ? (
                  <Navigate to="/dashboard" replace />
                ) : (
                  <Page className="auth-page-wrapper">
                    <SignupPage onAuthenticated={handleAuthenticated} />
                  </Page>
                )
              }
            />

            <Route path="/oauth2/callback" element={<Page><OAuth2CallbackPage /></Page>} />

            <Route
              path="/planner"
              element={
                <ProtectedRoute session={session}>
                  <Page><PlannerPage /></Page>
                </ProtectedRoute>
              }
            />

            <Route
              path="/dashboard"
              element={
                <ProtectedRoute session={session}>
                  <Page><DashboardPage /></Page>
                </ProtectedRoute>
              }
            />

            <Route
              path="/history"
              element={
                <ProtectedRoute session={session}>
                  <Page><HistoryPage /></Page>
                </ProtectedRoute>
              }
            />

            <Route
              path="/profile"
              element={
                <ProtectedRoute session={session}>
                  <Page><ProfilePage session={session} onLogout={handleLogout} /></Page>
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<Page><NotFoundPage /></Page>} />
          </Routes>

          <RoamoraCopilot />
          <Footer />
        </div>
      </BrowserRouter>
    </AppErrorBoundary>
  );
}
