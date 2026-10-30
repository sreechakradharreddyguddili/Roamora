import { apiRequest } from "./client";

function asList(result) {
  if (Array.isArray(result)) return result;
  if (Array.isArray(result?.data)) return result.data;
  if (Array.isArray(result?.content)) return result.content;
  return [];
}

export async function getTransportFeeds() {
  return asList(await apiRequest("/gtfs/feeds?active=true", { method: "GET" }));
}

export async function getLiveVehicles(feedId) {
  if (!feedId) return [];
  return asList(await apiRequest(`/gtfs/realtime/vehicles?feedId=${encodeURIComponent(feedId)}&limit=100`, { method: "GET" }));
}

export async function getLiveTripUpdates(feedId) {
  if (!feedId) return [];
  return asList(await apiRequest(`/gtfs/realtime/trip-updates?feedId=${encodeURIComponent(feedId)}&limit=100`, { method: "GET" }));
}

export async function getTransportAlerts(feedId) {
  if (!feedId) return [];
  return asList(await apiRequest(`/gtfs/realtime/alerts?feedId=${encodeURIComponent(feedId)}&limit=50`, { method: "GET" }));
}

export async function searchGtfsStops(feedId, query, limit = 10) {
  if (!feedId || !query || query.trim().length < 2) return [];
  const result = await apiRequest(`/gtfs/stops?feedId=${encodeURIComponent(feedId)}&query=${encodeURIComponent(query.trim())}&limit=${limit}`, { method: "GET" });
  return asList(result);
}

export async function getGtfsDepartures(feedId, stopId, limit = 12, date) {
  if (!feedId || !stopId) return null;
  const params = new URLSearchParams({ feedId, stopId, limit: String(limit) });
  if (date) params.set("date", date);
  return apiRequest(`/gtfs/departures?${params.toString()}`, { method: "GET" });
}

export async function searchTransportHubs({ city, country, query, limit = 50 } = {}) {
  const params = new URLSearchParams({ limit: String(limit) });
  if (city?.trim()) params.set("city", city.trim());
  if (country?.trim()) params.set("country", country.trim());
  if (query?.trim()) params.set("query", query.trim());
  return asList(await apiRequest(`/global-transport-places?${params.toString()}`, { method: "GET" }));
}

export async function searchTransportMapPlaces({ latitude, longitude, radiusKm = 25, limit = 60, type } = {}) {
  if (!Number.isFinite(Number(latitude)) || !Number.isFinite(Number(longitude))) return [];
  const params = new URLSearchParams({ latitude: String(latitude), longitude: String(longitude), radiusKm: String(radiusKm), limit: String(limit) });
  if (type) params.set("type", type);
  return asList(await apiRequest(`/places/transport-nearby?${params.toString()}`, { method: "GET" }));
}
