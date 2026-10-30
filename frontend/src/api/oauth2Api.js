const API_BASE = (
  import.meta.env.VITE_API_URL ||
  "http://localhost:8080/api/v1"
).replace(/\/$/, "");

const BACKEND_BASE = API_BASE.replace(
  /\/api\/v1$/,
  ""
);

export function getGoogleLoginUrl() {
  return `${BACKEND_BASE}/oauth2/authorization/google`;
}