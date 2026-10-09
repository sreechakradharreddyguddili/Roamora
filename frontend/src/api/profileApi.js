import { apiRequest } from "./client";

export async function getProfile() {
  const result = await apiRequest("/profile", {
    method: "GET",
  });

  if (!result?.success) {
    throw new Error(result?.message || "Unable to fetch your profile.");
  }

  return result.data || null;
}
