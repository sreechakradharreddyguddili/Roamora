import { useEffect, useMemo, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  useMap,
} from "react-leaflet";
import L from "leaflet";

import "leaflet/dist/leaflet.css";
import "./MapView.css";

const originIcon = new L.DivIcon({
  className: "roamora-map-marker",
  html: `
    <div class="roamora-marker origin-marker">
      <span>A</span>
    </div>
  `,
  iconSize: [36, 36],
  iconAnchor: [18, 18],
  popupAnchor: [0, -18],
});

const destinationIcon = new L.DivIcon({
  className: "roamora-map-marker",
  html: `
    <div class="roamora-marker destination-marker">
      <span>B</span>
    </div>
  `,
  iconSize: [36, 36],
  iconAnchor: [18, 18],
  popupAnchor: [0, -18],
});

function MapBounds({ points }) {
  const map = useMap();

  useEffect(() => {

    if (!points || points.length === 0) {
      return;
    }

    const bounds =
      L.latLngBounds(points);

    if (bounds.isValid()) {

      map.fitBounds(
        bounds,
        {
          padding: [40, 40],
          maxZoom: 12,
        }
      );
    }

  }, [map, points]);

  return null;
}

function parseGeometry(geometry) {

  if (!geometry) {
    return [];
  }

  try {

    const parsed =
      typeof geometry === "string"
        ? JSON.parse(geometry)
        : geometry;

    if (!parsed) {
      return [];
    }

    if (parsed.type === "Feature") {

      return parseGeometry(
        parsed.geometry
      );
    }

    if (parsed.type === "LineString") {

      return parsed.coordinates
        .filter(
          (coordinate) =>
            Array.isArray(coordinate) &&
            coordinate.length >= 2 &&
            Number.isFinite(
              Number(coordinate[0])
            ) &&
            Number.isFinite(
              Number(coordinate[1])
            )
        )
        .map(
          ([longitude, latitude]) => [
            Number(latitude),
            Number(longitude),
          ]
        );
    }

    if (
      parsed.type ===
      "MultiLineString"
    ) {

      return parsed.coordinates
        .flat()
        .filter(
          (coordinate) =>
            Array.isArray(coordinate) &&
            coordinate.length >= 2
        )
        .map(
          ([longitude, latitude]) => [
            Number(latitude),
            Number(longitude),
          ]
        );
    }

    if (Array.isArray(parsed)) {

      return parsed
        .filter(
          (coordinate) =>
            Array.isArray(coordinate) &&
            coordinate.length >= 2
        )
        .map(
          ([longitude, latitude]) => [
            Number(latitude),
            Number(longitude),
          ]
        );
    }

  } catch (error) {

    console.error(
      "Unable to parse route geometry:",
      error
    );
  }

  return [];
}

function MapControls({ point }) {
  const map = useMap();
  const [satellite, setSatellite] = useState(false);

  const openStreetView = () => {
    if (!point) return;
    const [lat, lng] = point;
    const url = `https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${encodeURIComponent(lat)},${encodeURIComponent(lng)}`;
    window.open(url, "_blank", "noopener,noreferrer");
  };

  const resetView = () => {
    map.setView(point || [20.5937, 78.9629], point ? Math.max(map.getZoom(), 8) : 3, {
      animate: true,
    });
  };

  const locateUser = () => {
    map.locate({ setView: true, maxZoom: 14, enableHighAccuracy: true });
  };

  return (
    <div className="roamora-map-actions" aria-label="Map controls">
      <button type="button" onClick={() => map.zoomIn()} title="Zoom in">+</button>
      <button type="button" onClick={() => map.zoomOut()} title="Zoom out">−</button>
      <button type="button" onClick={resetView} title="Reset journey view">⌖</button>
      <button type="button" onClick={locateUser} title="Use my location">◎</button>
      <button
        type="button"
        className={satellite ? "active" : ""}
        onClick={() => setSatellite((value) => !value)}
        title="Toggle satellite imagery"
      >
        SAT
      </button>
      <button type="button" onClick={openStreetView} disabled={!point} title="Open 360 street view">360°</button>
    </div>
  );
}

function formatDistance(distance) {

  if (
    distance === null ||
    distance === undefined
  ) {
    return "—";
  }

  return `${Number(distance).toFixed(1)} km`;
}

function formatDuration(minutes) {

  if (
    minutes === null ||
    minutes === undefined
  ) {
    return "—";
  }

  const totalMinutes =
    Math.round(
      Number(minutes)
    );

  const hours =
    Math.floor(
      totalMinutes / 60
    );

  const remainingMinutes =
    totalMinutes % 60;

  if (hours === 0) {
    return `${remainingMinutes} min`;
  }

  if (remainingMinutes === 0) {
    return `${hours} hr`;
  }

  return `${hours} hr ${remainingMinutes} min`;
}

export default function MapView({
  analysis,
  locations,
  selectedRouteKey,
  onRouteSelect,
}) {

  const alternatives =
    useMemo(
      () =>
        analysis?.alternativeRoutes ||
        analysis?.alternatives ||
        [],
      [analysis]
    );

  const selectedRoute =
    alternatives.find(
      (route) =>
        route.routeKey ===
        selectedRouteKey
    ) ||
    alternatives.find(
      (route) =>
        route.recommended
    ) ||
    alternatives[0];

  const selectedPoints =
    useMemo(
      () =>
        parseGeometry(
          selectedRoute?.geometry
        ),
      [selectedRoute]
    );

  const allRoutePoints =
    useMemo(
      () => {

        return alternatives.map(
          (route) => ({
            route,
            points:
              parseGeometry(
                route.geometry
              ),
          })
        );

      },
      [alternatives]
    );

  const originPoint =
    selectedPoints.length > 0
      ? selectedPoints[0]
      : null;

  const destinationPoint =
    selectedPoints.length > 1
      ? selectedPoints[
          selectedPoints.length - 1
        ]
      : null;

  const mapCenter =
    originPoint ||
    destinationPoint ||
    [20.5937, 78.9629];

  const boundsPoints =
    selectedPoints;

  const mapAvailable =
    analysis?.mapAvailable !== false;

  return (
    <section className="route-map-section">

      <div className="route-map-header">

        <div>

          <span className="route-map-eyebrow">
            ROUTE VISUALIZATION
          </span>

          <h2>
            Your journey on the map
          </h2>

          <p>
            Select a route to compare the
            available journey options.
          </p>

        </div>

        {selectedRoute && (
          <div className="route-map-summary">

            <div>
              <span>
                Distance
              </span>

              <strong>
                {formatDistance(
                  selectedRoute.distanceKm
                )}
              </strong>
            </div>

            <div>
              <span>
                Duration
              </span>

              <strong>
                {formatDuration(
                  selectedRoute.durationMinutes
                )}
              </strong>
            </div>

            <div>
              <span>
                Risk
              </span>

              <strong>
                {
                  selectedRoute.riskLevel ||
                  "—"
                }
              </strong>
            </div>

          </div>
        )}

      </div>

      {alternatives.length > 0 &&
        mapAvailable && (
          <div className="route-map-options">

            {alternatives.map(
              (route, index) => {

                const active =
                  route.routeKey ===
                  (
                    selectedRoute?.routeKey ||
                    selectedRouteKey
                  );

                return (
                  <button
                    key={
                      route.routeKey ||
                      index
                    }
                    type="button"
                    className={
                      `route-map-option ${
                        active
                          ? "active"
                          : ""
                      }`
                    }
                    onClick={() =>
                      onRouteSelect?.(
                        route.routeKey
                      )
                    }
                  >

                    <span className="route-map-option-number">
                      {index + 1}
                    </span>

                    <span className="route-map-option-content">

                      <strong>
                        {
                          route.recommended
                            ? "Recommended route"
                            : `Route ${index + 1}`
                        }
                      </strong>

                      <span>
                        {
                          formatDistance(
                            route.distanceKm
                          )
                        }

                        {" • "}

                        {
                          formatDuration(
                            route.durationMinutes
                          )
                        }
                      </span>

                    </span>

                    {route.recommended && (
                      <span className="route-map-recommended">
                        Recommended
                      </span>
                    )}

                  </button>
                );
              }
            )}

          </div>
        )}

      {!analysis && (
        <div className="route-map-container">

          <div className="route-map-empty">

            <div className="route-map-empty-icon">
              🗺️
            </div>

            <strong>
              Your route will appear here
            </strong>

            <span>
              Enter your starting point and
              destination to visualize the journey.
            </span>

          </div>

        </div>
      )}

      {analysis && !mapAvailable && (
        <div className="route-map-container">

          <div className="route-map-unavailable">

            <div className="route-map-unavailable-icon">
              🗺️
            </div>

            <span className="route-map-unavailable-label">
              MAP NOT AVAILABLE
            </span>

            <h3>
              No mapped route for this transport
            </h3>

            <p>
              {
                analysis?.transportMode
                  ? `${analysis.transportMode} routing is not currently available for this journey.`
                  : "A mapped route is not currently available for this journey."
              }
            </p>

            <small>
              Roamora does not replace an unsupported
              transport mode with a car route.
            </small>

          </div>

        </div>
      )}

      {analysis &&
        mapAvailable && (
          <div className="route-map-container">

            <MapContainer
              center={mapCenter}
              zoom={6}
              scrollWheelZoom
              className="route-map"
            >

              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url={import.meta.env.VITE_MAP_TILE_URL || "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"}
              />

              <MapControls point={originPoint || destinationPoint} />

              <MapBounds
                points={boundsPoints}
              />

              {allRoutePoints.map(
                (
                  { route, points },
                  index
                ) => {

                  if (
                    points.length < 2
                  ) {
                    return null;
                  }

                  const active =
                    route.routeKey ===
                    selectedRoute?.routeKey;

                  return (
                    <Polyline
                      key={
                        route.routeKey ||
                        index
                      }
                      positions={points}
                      pathOptions={{
                        weight:
                          active
                            ? 7
                            : 4,
                        opacity:
                          active
                            ? 0.95
                            : 0.35,
                        className:
                          active
                            ? "route-line-active"
                            : "route-line-inactive",
                      }}
                      eventHandlers={{
                        click: () =>
                          onRouteSelect?.(
                            route.routeKey
                          ),
                      }}
                    />
                  );
                }
              )}

              {originPoint && (
                <Marker
                  position={originPoint}
                  icon={originIcon}
                >
                  <Popup>

                    <strong>
                      Starting point
                    </strong>

                    <br />

                    {
                      locations?.origin ||
                      "Origin"
                    }

                  </Popup>
                </Marker>
              )}

              {destinationPoint && (
                <Marker
                  position={
                    destinationPoint
                  }
                  icon={destinationIcon}
                >
                  <Popup>

                    <strong>
                      Destination
                    </strong>

                    <br />

                    {
                      locations?.destination ||
                      "Destination"
                    }

                  </Popup>
                </Marker>
              )}

            </MapContainer>

            {selectedPoints.length < 2 && (
              <div className="route-map-empty">

                <div className="route-map-empty-icon">
                  📍
                </div>

                <strong>
                  Route geometry unavailable
                </strong>

                <span>
                  The routing provider returned
                  no usable map geometry.
                </span>

              </div>
            )}

          </div>
        )}

    </section>
  );
}