import { refreshToken as refreshAccessToken } from "./authApi";
import { storage } from "../utils/storage";

const API_BASE = (
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080/api/v1"
).replace(/\/+$/, "");

let refreshPromise = null;

async function getValidAccessToken() {
  const session = storage.getSession();

  if (!session?.token) {
    return null;
  }

  return session.token;
}

async function performRefresh() {
  const session = storage.getSession();

  if (!session?.refreshToken) {
    throw new Error(
      "Your session has expired."
    );
  }

  if (!refreshPromise) {
    refreshPromise = refreshAccessToken(
      session.refreshToken
    )
      .then((result) => {
        const data =
          result?.data || result;

        const nextSession = {
          ...session,
          token:
            data?.token ||
            data?.accessToken ||
            session.token,
          refreshToken:
            data?.refreshToken ||
            session.refreshToken,
        };

        storage.setSession(nextSession);

        return nextSession.token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

export async function apiRequest(
  path,
  options = {},
  retry = true
) {
  const token =
    await getValidAccessToken();

  const headers = new Headers(
    options.headers || {}
  );

  if (
    options.body &&
    !headers.has("Content-Type")
  ) {
    headers.set(
      "Content-Type",
      "application/json"
    );
  }

  headers.set("Accept", "application/json");

  if (token) {
    headers.set(
      "Authorization",
      `Bearer ${token}`
    );
  }

  let response;

  try {
    response = await fetch(
      `${API_BASE}${path}`,
      {
        ...options,
        headers,
      }
    );
  } catch {
    throw new Error(
      "Roamora could not reach the server. Please check your connection and try again."
    );
  }

  if (
    response.status === 401 &&
    retry &&
    storage.getSession()?.refreshToken
  ) {
    try {
      const newToken =
        await performRefresh();

      const retryHeaders =
        new Headers(
          options.headers || {}
        );

      if (
        options.body &&
        !retryHeaders.has(
          "Content-Type"
        )
      ) {
        retryHeaders.set(
          "Content-Type",
          "application/json"
        );
      }

      retryHeaders.set(
        "Accept",
        "application/json"
      );

      retryHeaders.set(
        "Authorization",
        `Bearer ${newToken}`
      );

      try {
        response = await fetch(
          `${API_BASE}${path}`,
          {
            ...options,
            headers: retryHeaders,
          }
        );
      } catch {
        throw new Error(
          "Roamora could not reach the server. Please check your connection and try again."
        );
      }
    } catch {
      storage.clearSession();

      throw new Error(
        "Your session has expired. Please sign in again."
      );
    }
  }

  const body = await response
    .json()
    .catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message ||
        body?.error ||
        "Request failed."
    );
  }

  return body;
}

export { API_BASE };