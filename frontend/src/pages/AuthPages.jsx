import { useState } from "react";
import {
  ArrowRight,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Map,
  User,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { login, signup } from "../api/authApi";
import { getGoogleLoginUrl } from "../api/oauth2Api";
import { storage } from "../utils/storage";

import "./AuthPages.css";

const GOOGLE_AUTH_ENABLED =
  import.meta.env.VITE_GOOGLE_AUTH_ENABLED === "true";

function getErrorFromUrl(search) {
  const params = new URLSearchParams(search);
  return params.get("error") || "";
}

function AuthLayout({
  mode,
  children,
}) {
  const isLogin = mode === "login";

  return (
    <main className="auth-page">
      <section className="auth-brand-panel auth-static-panel">
        <div className="auth-brand-content">
          <div className="auth-brand-mark">
            <Map size={22} />
          </div>

          <span className="auth-brand-eyebrow">
            ROAMORA
          </span>

          <h1>
            Travel with
            <br />
            more context.
          </h1>

          <p>
            Weather-aware route planning that helps
            you understand your journey before you
            start it.
          </p>

          <div className="auth-brand-points">
            <div>
              <span />
              <p>
                Compare alternative routes
              </p>
            </div>

            <div>
              <span />
              <p>
                Understand weather conditions
              </p>
            </div>

            <div>
              <span />
              <p>
                Review travel risk
              </p>
            </div>
          </div>
        </div>

        <div className="auth-brand-footer">
          <span>
            {isLogin
              ? "WELCOME BACK"
              : "START YOUR JOURNEY"}
          </span>
        </div>
      </section>

      <section className="auth-form-panel auth-static-panel">
        {children}
      </section>
    </main>
  );
}

function AuthHeader({ mode }) {
  const isLogin = mode === "login";

  return (
    <div className="auth-header">
      <span>
        {isLogin
          ? "WELCOME BACK"
          : "CREATE ACCOUNT"}
      </span>

      <h2>
        {isLogin
          ? "Sign in to Roamora"
          : "Create your account"}
      </h2>

      <p>
        {isLogin
          ? "Continue planning weather-aware journeys."
          : "Start planning smarter journeys with Roamora."}
      </p>
    </div>
  );
}

function GoogleButton() {
  if (!GOOGLE_AUTH_ENABLED) {
    return null;
  }

  const handleGoogleLogin = () => {
    window.location.href =
      getGoogleLoginUrl();
  };

  return (
    <button
      type="button"
      className="auth-google-button"
      onClick={handleGoogleLogin}
    >
      <span className="google-logo">
        G
      </span>

      <span>
        Continue with Google
      </span>
    </button>
  );
}

function Divider() {
  return (
    <div className="auth-divider">
      <span />
      <strong>OR</strong>
      <span />
    </div>
  );
}

function LoginPage({ onAuthenticated }) {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] =
    useState("");

  const [showPassword, setShowPassword] =
    useState(false);

  const [error, setError] = useState(() =>
    getErrorFromUrl(location.search)
  );

  const [loading, setLoading] =
    useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    const cleanEmail = email
      .trim()
      .toLowerCase();

    if (!cleanEmail || !password) {
      setError(
        "Please enter your email and password."
      );
      return;
    }

    setLoading(true);

    try {
      const result = await login({
        email: cleanEmail,
        password,
      });

      const data =
        result?.data || result;

      storage.setSession({
        id: data?.id ?? null,
        name: data?.name || "",
        email:
          data?.email || cleanEmail,
        role: data?.role || "USER",
        token:
          data?.token ||
          data?.accessToken ||
          "",
        refreshToken:
          data?.refreshToken || "",
      });

      onAuthenticated?.();

      const returnTo = location.state?.from;
      const destination = returnTo
        ? `${returnTo.pathname || "/planner"}${returnTo.search || ""}${returnTo.hash || ""}`
        : "/planner";
      navigate(destination, { replace: true });
    } catch (requestError) {
      setError(
        requestError.message ||
          "Unable to sign in."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout mode="login">
      <div className="auth-form-container">
        <AuthHeader mode="login" />

        {GOOGLE_AUTH_ENABLED && (
          <>
            <GoogleButton />
            <Divider />
          </>
        )}

        <form
          className="auth-form"
          onSubmit={handleSubmit}
        >
          <div className="auth-field">
            <label htmlFor="login-email">
              Email address
            </label>

            <div className="auth-input">
              <Mail size={17} />

              <input
                id="login-email"
                type="email"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  setError("");
                }}
                placeholder="you@example.com"
                autoComplete="email"
                disabled={loading}
              />
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="login-password">
              Password
            </label>

            <div className="auth-input">
              <Lock size={17} />

              <input
                id="login-password"
                type={
                  showPassword
                    ? "text"
                    : "password"
                }
                value={password}
                onChange={(event) => {
                  setPassword(
                    event.target.value
                  );
                  setError("");
                }}
                placeholder="Enter your password"
                autoComplete="current-password"
                disabled={loading}
              />

              <button
                type="button"
                onClick={() =>
                  setShowPassword(
                    (value) => !value
                  )
                }
                aria-label={
                  showPassword
                    ? "Hide password"
                    : "Show password"
                }
              >
                {showPassword ? (
                  <EyeOff size={17} />
                ) : (
                  <Eye size={17} />
                )}
              </button>
            </div>
          </div>

          {error && (
            <div
              className="auth-error"
              role="alert"
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            <span>
              {loading
                ? "Signing in..."
                : "Sign in"}
            </span>

            {!loading && (
              <ArrowRight size={18} />
            )}
          </button>
        </form>

        <div className="auth-switch">
          <span>
            Don't have an account?
          </span>

          <Link to="/signup" state={location.state}>
            Create one
          </Link>
        </div>
      </div>
    </AuthLayout>
  );
}

function SignupPage({ onAuthenticated }) {
  const navigate = useNavigate();
  const location = useLocation();

  const [name, setName] = useState("");
  const [email, setEmail] =
    useState("");
  const [password, setPassword] =
    useState("");
  const [confirmPassword, setConfirmPassword] =
    useState("");

  const [showPassword, setShowPassword] =
    useState(false);

  const [error, setError] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");

    const cleanName = name.trim();
    const cleanEmail = email
      .trim()
      .toLowerCase();

    if (!cleanName) {
      setError(
        "Please enter your name."
      );
      return;
    }

    if (!cleanEmail) {
      setError(
        "Please enter your email address."
      );
      return;
    }

    if (password.length < 6) {
      setError(
        "Password must contain at least 6 characters."
      );
      return;
    }

    if (password !== confirmPassword) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    setLoading(true);

    try {
      const result = await signup({
        name: cleanName,
        email: cleanEmail,
        password,
      });

      const data =
        result?.data || result;

      storage.setSession({
        id: data?.id ?? null,
        name:
          data?.name || cleanName,
        email:
          data?.email || cleanEmail,
        role: data?.role || "USER",
        token:
          data?.token ||
          data?.accessToken ||
          "",
        refreshToken:
          data?.refreshToken || "",
      });

      onAuthenticated?.();

      const returnTo = location.state?.from;
      const destination = returnTo
        ? `${returnTo.pathname || "/planner"}${returnTo.search || ""}${returnTo.hash || ""}`
        : "/planner";
      navigate(destination, { replace: true });
    } catch (requestError) {
      setError(
        requestError.message ||
          "Unable to create your account."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout mode="signup">
      <div className="auth-form-container">
        <AuthHeader mode="signup" />

        {GOOGLE_AUTH_ENABLED && (
          <>
            <GoogleButton />
            <Divider />
          </>
        )}

        <form
          className="auth-form"
          onSubmit={handleSubmit}
        >
          <div className="auth-field">
            <label htmlFor="signup-name">
              Full name
            </label>

            <div className="auth-input">
              <User size={17} />

              <input
                id="signup-name"
                type="text"
                value={name}
                onChange={(event) => {
                  setName(event.target.value);
                  setError("");
                }}
                placeholder="Your name"
                autoComplete="name"
                disabled={loading}
              />
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="signup-email">
              Email address
            </label>

            <div className="auth-input">
              <Mail size={17} />

              <input
                id="signup-email"
                type="email"
                value={email}
                onChange={(event) => {
                  setEmail(
                    event.target.value
                  );
                  setError("");
                }}
                placeholder="you@example.com"
                autoComplete="email"
                disabled={loading}
              />
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="signup-password">
              Password
            </label>

            <div className="auth-input">
              <Lock size={17} />

              <input
                id="signup-password"
                type={
                  showPassword
                    ? "text"
                    : "password"
                }
                value={password}
                onChange={(event) => {
                  setPassword(
                    event.target.value
                  );
                  setError("");
                }}
                placeholder="At least 6 characters"
                autoComplete="new-password"
                disabled={loading}
              />

              <button
                type="button"
                onClick={() =>
                  setShowPassword(
                    (value) => !value
                  )
                }
                aria-label={
                  showPassword
                    ? "Hide password"
                    : "Show password"
                }
              >
                {showPassword ? (
                  <EyeOff size={17} />
                ) : (
                  <Eye size={17} />
                )}
              </button>
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="signup-confirm-password">
              Confirm password
            </label>

            <div className="auth-input">
              <Lock size={17} />

              <input
                id="signup-confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(event) => {
                  setConfirmPassword(
                    event.target.value
                  );
                  setError("");
                }}
                placeholder="Repeat your password"
                autoComplete="new-password"
                disabled={loading}
              />
            </div>
          </div>

          {error && (
            <div
              className="auth-error"
              role="alert"
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            <span>
              {loading
                ? "Creating account..."
                : "Create account"}
            </span>

            {!loading && (
              <ArrowRight size={18} />
            )}
          </button>
        </form>

        <div className="auth-switch">
          <span>
            Already have an account?
          </span>

          <Link to="/login" state={location.state}>
            Sign in
          </Link>
        </div>
      </div>
    </AuthLayout>
  );
}

export { LoginPage, SignupPage };