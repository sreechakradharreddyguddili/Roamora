const API_BASE = (
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080/api/v1"
).replace(/\/$/, "");

async function readBody(response) {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function friendlyNetworkError() {
  return new Error(
    "Roamora could not reach the server. Please check that the backend is running and try again."
  );
}

async function authRequest(path, payload) {
  let response;

  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify(payload),
    });
  } catch {
    throw friendlyNetworkError();
  }

  const body = await readBody(response);

  if (!response.ok) {
    const message =
      body?.message ||
      body?.error ||
      (response.status === 401
        ? "Invalid email or password."
        : response.status === 409
          ? "An account with this email already exists."
          : response.status >= 500
            ? "Roamora is having trouble right now. Please try again shortly."
            : "Authentication request could not be completed.");

    throw new Error(message);
  }

  return body;
}

export const login = (credentials) =>
  authRequest("/auth/login", credentials);

export const signup = (payload) =>
  authRequest("/auth/register", payload);

export const refreshToken = (refreshTokenValue) =>
  authRequest("/auth/refresh", {
    refreshToken: refreshTokenValue,
  });

export const logout = (refreshTokenValue) =>
  authRequest("/auth/logout", {
    refreshToken: refreshTokenValue,
  });
