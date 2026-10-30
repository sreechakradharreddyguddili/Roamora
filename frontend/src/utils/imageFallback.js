export function handleImageError(event) {
  const image = event.currentTarget;
  if (image.dataset.fallbackApplied === "true") return;
  image.dataset.fallbackApplied = "true";
  image.src = "/images/roamora-photo-placeholder.svg";
  image.alt = "Travel photo unavailable";
}
