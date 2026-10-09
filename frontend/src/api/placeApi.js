import { apiRequest } from "./client";

export async function getNearbyPlaces({
  latitude,
  longitude,
  category,
  radiusKm = 8,
  limit = 24,
}) {
  const params = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    radiusKm: String(radiusKm),
    limit: String(limit),
  });

  if (category) {
    params.set("category", category);
  }

  const result = await apiRequest(
    `/places/nearby?${params.toString()}`,
    { method: "GET" }
  );

  if (Array.isArray(result)) {
    return result;
  }

  if (Array.isArray(result?.data)) {
    return result.data;
  }

  return [];
}
