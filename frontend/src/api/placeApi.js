import { apiRequest } from "./client";

// Reuse nearby results for the same coordinates and filters during a session.
const nearbyCache = new Map();
const nearbyRequests = new Map();
const NEARBY_TTL_MS = 3 * 60 * 1000;

export async function getNearbyPlaces({
  latitude,
  longitude,
  category,
  radiusKm = 8,
  limit = 24,
}) {
  const cacheKey = [Number(latitude).toFixed(3), Number(longitude).toFixed(3), category || "ALL", radiusKm, limit].join(":");
  const cached = nearbyCache.get(cacheKey);
  if (cached && Date.now() - cached.at < (cached.ttl || NEARBY_TTL_MS)) return cached.value;
  if (nearbyRequests.has(cacheKey)) return nearbyRequests.get(cacheKey);

  const params = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    radiusKm: String(radiusKm),
    limit: String(limit),
  });

  if (category) {
    params.set("category", category);
  }

  const request = apiRequest(`/places/nearby?${params.toString()}`, { method: "GET" })
    .then((result) => {
      const values = Array.isArray(result) ? result : Array.isArray(result?.data) ? result.data : [];
      nearbyCache.set(cacheKey, { at: Date.now(), value: values, ttl: values.length ? NEARBY_TTL_MS : 10000 });
      if (nearbyCache.size > 100) nearbyCache.delete(nearbyCache.keys().next().value);
      return values;
    })
    .finally(() => nearbyRequests.delete(cacheKey));
  nearbyRequests.set(cacheKey, request);
  return request;
}
