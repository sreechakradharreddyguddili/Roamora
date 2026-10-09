import { apiRequest } from "./client";

function getPageData(result) {
  return (
    result?.data || {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 0,
    }
  );
}

export async function planRoute({
  origin,
  destination,
  originLocation,
  destinationLocation,
  transportMode,
  departureDate,
  departureTime,
}) {
  const result = await apiRequest(
    "/routes/plan",
    {
      method: "POST",
      body: JSON.stringify({
        origin,
        destination,
        transportMode,
        departureDate,
        departureTime,

        originLatitude:
          originLocation?.latitude ?? null,

        originLongitude:
          originLocation?.longitude ?? null,

        destinationLatitude:
          destinationLocation?.latitude ?? null,

        destinationLongitude:
          destinationLocation?.longitude ?? null,
      }),
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Route analysis was not successful."
    );
  }

  return result.data;
}

export async function searchLocations(
  query
) {
  const cleanQuery = query?.trim();

  if (
    !cleanQuery ||
    cleanQuery.length < 2
  ) {
    return [];
  }

  const result = await apiRequest(
    `/locations/suggestions?query=${encodeURIComponent(
      cleanQuery
    )}`,
    {
      method: "GET",
    }
  );

  if (Array.isArray(result)) {
    return result;
  }

  if (Array.isArray(result?.data)) {
    return result.data;
  }

  return [];
}


export async function resolveLocation(location) {
  const cleanLocation = location?.trim();

  if (!cleanLocation || cleanLocation.length < 2) {
    throw new Error("Enter a destination first.");
  }

  const result = await apiRequest(
    `/locations/search?city=${encodeURIComponent(cleanLocation)}`,
    { method: "GET" }
  );

  const data = result?.data || result;

  if (
    !data ||
    data.latitude === undefined ||
    data.longitude === undefined
  ) {
    throw new Error("We could not resolve that destination. Try a city or landmark name.");
  }

  return data;
}

export async function getRoutes(
  page = 0,
  size = 20
) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const result = await apiRequest(
    `/routes?${query.toString()}`,
    {
      method: "GET",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to load routes."
    );
  }

  return getPageData(result);
}

export async function getRouteById(
  routeId
) {
  const result = await apiRequest(
    `/routes/${routeId}`,
    {
      method: "GET",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to load route."
    );
  }

  return result.data;
}

export async function saveRoute(
  routeId
) {
  const result = await apiRequest(
    `/routes/${routeId}/save`,
    {
      method: "POST",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to save route."
    );
  }

  return result.data;
}

export async function unsaveRoute(
  routeId
) {
  const result = await apiRequest(
    `/routes/${routeId}/save`,
    {
      method: "DELETE",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to remove saved route."
    );
  }

  return result.data;
}

export async function deleteRoute(
  routeId
) {
  const result = await apiRequest(
    `/routes/${routeId}`,
    {
      method: "DELETE",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to delete route."
    );
  }

  return result;
}

export async function clearRouteHistory() {
  const result = await apiRequest(
    "/routes/history",
    {
      method: "DELETE",
    }
  );

  if (!result?.success) {
    throw new Error(
      result?.message ||
        "Unable to clear route history."
    );
  }

  return result.data;
}

export async function getSavedRoutes(page = 0, size = 20) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const result = await apiRequest(`/routes/saved?${query.toString()}`, {
    method: "GET",
  });

  if (!result?.success) {
    throw new Error(
      result?.message || "Unable to load saved routes."
    );
  }

  return getPageData(result);
}