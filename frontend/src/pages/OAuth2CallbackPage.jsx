import { useEffect, useState } from "react";
import { AlertCircle, Loader2, Map } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

import { storage } from "../utils/storage";

import "./OAuth2CallbackPage.css";

function OAuth2CallbackPage() {
  const navigate = useNavigate();

  const [error] = useState(() => {
    const params = new URLSearchParams(
      window.location.search
    );
    const oauthError = params.get("error");
    const token = params.get("token");
    const refreshToken = params.get("refreshToken");

    return oauthError ||
      !token ||
      !refreshToken
      ? oauthError ||
          "Google authentication failed. Please try again."
      : "";
  });

  useEffect(() => {
    const params = new URLSearchParams(
      window.location.search
    );

    const token = params.get("token");
    const refreshToken =
      params.get("refreshToken");

    const id = params.get("id");
    const name = params.get("name");
    const email = params.get("email");
    const role = params.get("role");

    const oauthError = params.get("error");

    if (
      oauthError ||
      !token ||
      !refreshToken
    ) {
      return;
    }

    storage.setSession({
      id: id ? Number(id) : null,
      name: name || "",
      email: email || "",
      role: role || "USER",
      token,
      refreshToken,
      provider: "google",
    });

    window.history.replaceState(
      {},
      document.title,
      "/oauth2/callback"
    );

    navigate("/planner", {
      replace: true,
    });
  }, [navigate]);

  if (error) {
    return (
      <main className="oauth-callback-page">
        <section className="oauth-callback-card">
          <div className="oauth-callback-error-icon">
            <AlertCircle size={25} />
          </div>

          <span>AUTHENTICATION ERROR</span>

          <h1>
            We couldn't sign you in.
          </h1>

          <p>{error}</p>

          <Link to="/login">
            Return to sign in
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="oauth-callback-page">
      <section className="oauth-callback-card">
        <div className="oauth-callback-logo">
          <Map size={23} />
        </div>

        <span>ROAMORA</span>

        <h1>
          Signing you in...
        </h1>

        <p>
          Completing your Google authentication
          and preparing your journey planner.
        </p>

        <Loader2
          className="oauth-callback-spinner"
          size={23}
        />
      </section>
    </main>
  );
}

export default OAuth2CallbackPage;