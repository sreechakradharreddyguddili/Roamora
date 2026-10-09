import { apiRequest } from "./client";

export async function askRoamoraIntelligence({ question, journeyContext }) {
  const result = await apiRequest("/intelligence/chat", {
    method: "POST",
    body: JSON.stringify({
      question,
      journeyContext,
    }),
  });

  if (!result?.success || !result?.data) {
    throw new Error(result?.message || "Roamora Intelligence is unavailable.");
  }

  return result.data;
}
