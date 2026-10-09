package com.weatherwise.routing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weatherwise.enums.TransportMode;
import com.weatherwise.location.LocationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RoutingService(
            @Value("${app.routing.base-url}")
            String baseUrl,
            ObjectMapper objectMapper
    ) {

        this.objectMapper = objectMapper;

        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader(
                                "User-Agent",
                                "Roamora-Journey-Intelligence/1.0"
                        )
                        /*
                         * Prevent HTTP compression problems when
                         * decoding the OSRM JSON response.
                         */
                        .defaultHeader(
                                "Accept-Encoding",
                                "identity"
                        )
                        .defaultHeader(
                                "Accept",
                                "application/json"
                        )
                        .build();
    }

    /**
     * Main routing entry point.
     *
     * Only transport modes that have a real road-routing
     * profile configured are sent to OSRM.
     *
     * All other modes return an unavailable response.
     *
     * We NEVER replace an unsupported mode with CAR routing.
     */
    public RoutingResponse calculateRoute(
            LocationResponse origin,
            LocationResponse destination,
            TransportMode transportMode
    ) {

        if (origin == null || destination == null) {

            return unavailableResponse(
                    transportMode,
                    "Route locations are not available."
            );
        }

        if (transportMode == null) {

            return unavailableResponse(
                    null,
                    "Transport mode is not available."
            );
        }

        /*
         * Only actual road modes reach OSRM.
         */
        if (!supportsRoadRouting(transportMode)) {

            return unavailableResponse(
                    transportMode,
                    getUnavailableMessage(transportMode)
            );
        }

        String profile =
                getProfile(
                        transportMode
                );

        String coordinates =
                origin.getLongitude()
                        + ","
                        + origin.getLatitude()
                        + ";"
                        + destination.getLongitude()
                        + ","
                        + destination.getLatitude();

        try {

            Map<?, ?> response =
                    restClient.get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path(
                                                            "/route/v1/{profile}/{coordinates}"
                                                    )
                                                    .queryParam(
                                                            "alternatives",
                                                            "3"
                                                    )
                                                    .queryParam(
                                                            "steps",
                                                            "false"
                                                    )
                                                    .queryParam(
                                                            "geometries",
                                                            "geojson"
                                                    )
                                                    .queryParam(
                                                            "overview",
                                                            "full"
                                                    )
                                                    .build(
                                                            profile,
                                                            coordinates
                                                    )
                            )
                            .retrieve()
                            .body(Map.class);

            return parseResponse(
                    response,
                    transportMode
            );

        } catch (RestClientException exception) {

            /*
             * Do not let an external routing-provider failure
             * break the entire route-planning API.
             *
             * No fake route is returned.
             */
            return unavailableResponse(
                    transportMode,
                    "The road routing provider is temporarily unavailable."
            );
        }
    }

    /**
     * Parse OSRM response.
     */
    private RoutingResponse parseResponse(
            Map<?, ?> response,
            TransportMode transportMode
    ) {

        if (response == null) {

            return unavailableResponse(
                    transportMode,
                    "The road routing provider returned no response."
            );
        }

        Object codeObject =
                response.get("code");

        String code =
                codeObject != null
                        ? codeObject.toString()
                        : null;

        /*
         * OSRM couldn't find a road route.
         */
        if (!"Ok".equalsIgnoreCase(code)) {

            if ("NoRoute".equalsIgnoreCase(code)) {

                return unavailableResponse(
                        transportMode,
                        "No mapped road route is available between these locations."
                );
            }

            return unavailableResponse(
                    transportMode,
                    "The road routing provider could not calculate a route."
            );
        }

        Object routesObject =
                response.get("routes");

        if (!(routesObject instanceof List<?> routes)
                || routes.isEmpty()) {

            return unavailableResponse(
                    transportMode,
                    "No mapped road route is available between these locations."
            );
        }

        List<RoutingOption> alternatives =
                new ArrayList<>();

        for (
                int i = 0;
                i < routes.size();
                i++
        ) {

            Object routeObject =
                    routes.get(i);

            if (!(routeObject
                    instanceof Map<?, ?> route)) {

                continue;
            }

            double distance =
                    number(
                            route.get("distance")
                    );

            double duration =
                    number(
                            route.get("duration")
                    );

            /*
             * OSRM returns GeoJSON here because the request
             * contains:
             *
             * geometries=geojson
             *
             * Jackson deserializes the geometry object into
             * a Map. Calling Map.toString() would produce:
             *
             * {coordinates=[[...]], type=LineString}
             *
             * which is NOT valid JSON.
             *
             * Serialize it back using Jackson so the API
             * returns valid GeoJSON JSON text.
             */
            String geometry =
                    serializeGeometry(
                            route.get("geometry")
                    );

            /*
             * A route without valid geometry cannot be
             * displayed as a real map route.
             */
            if (geometry == null
                    || geometry.isBlank()) {

                continue;
            }

            alternatives.add(
                    new RoutingOption(
                            "ROUTE_" + (i + 1),
                            distance / 1000.0,
                            (int)
                                    Math.ceil(
                                            duration / 60.0
                                    ),
                            geometry
                    )
            );
        }

        if (alternatives.isEmpty()) {

            return unavailableResponse(
                    transportMode,
                    "The routing provider did not return usable map geometry."
            );
        }

        RoutingOption primary =
                alternatives.get(0);

        return new RoutingResponse(
                primary.distanceKm(),
                primary.durationMinutes(),
                primary.geometry(),
                alternatives,
                true,
                "Map route available."
        );
    }

    /**
     * Converts the OSRM geometry object into valid
     * JSON text.
     *
     * Expected result:
     *
     * {
     *   "coordinates": [...],
     *   "type": "LineString"
     * }
     */
    private String serializeGeometry(
            Object geometry
    ) {

        if (geometry == null) {
            return null;
        }

        if (geometry instanceof String string) {

            if (string.isBlank()) {
                return null;
            }

            /*
             * If a provider already returned JSON text,
             * validate that it is JSON before returning it.
             */
            try {

                objectMapper.readTree(string);

                return string;

            } catch (JsonProcessingException exception) {

                return null;
            }
        }

        try {

            return objectMapper.writeValueAsString(
                    geometry
            );

        } catch (JsonProcessingException exception) {

            return null;
        }
    }

    /**
     * Determines which modes can currently use
     * the configured OSRM road routing provider.
     *
     * IMPORTANT:
     * Do not add TRAIN / METRO / BUS / FLIGHT here.
     *
     * Those require actual transit/flight providers.
     */
    private boolean supportsRoadRouting(
            TransportMode transportMode
    ) {

        return transportMode == TransportMode.CAR
                || transportMode == TransportMode.MOTORCYCLE
                || transportMode == TransportMode.TAXI
                || transportMode == TransportMode.AUTO_RICKSHAW
                || transportMode == TransportMode.BICYCLE
                || transportMode == TransportMode.SCOOTER
                || transportMode == TransportMode.WALKING;
    }

    /**
     * Maps supported transport modes to actual
     * OSRM profiles.
     */
    private String getProfile(
            TransportMode transportMode
    ) {

        return switch (transportMode) {

            /*
             * OSRM driving profile.
             *
             * Taxi, motorcycle, auto-rickshaw and scooter
             * are represented using the road-driving network
             * because there is no separate profile configured
             * in this application.
             */
            case CAR,
                 MOTORCYCLE,
                 TAXI,
                 AUTO_RICKSHAW,
                 SCOOTER ->
                    "driving";

            /*
             * Bicycle routing.
             */
            case BICYCLE ->
                    "bike";

            /*
             * Walking routing.
             */
            case WALKING ->
                    "foot";

            /*
             * These modes must NEVER reach OSRM.
             */
            case BUS,
                 COACH,
                 METRO,
                 SUBWAY,
                 TRAM,
                 LIGHT_RAIL,
                 TRAIN,
                 COMMUTER_RAIL,
                 HIGH_SPEED_TRAIN,
                 MONORAIL,
                 FLIGHT,
                 FERRY,
                 SHIP,
                 MULTIMODAL ->

                    throw new IllegalArgumentException(
                            "Transport mode is not supported by OSRM: "
                                    + transportMode
                    );
        };
    }

    /**
     * Returns a clear explanation for modes for which
     * this routing provider is not configured.
     */
    private String getUnavailableMessage(
            TransportMode transportMode
    ) {

        if (transportMode == null) {

            return "Transport mode is not available.";
        }

        return switch (transportMode) {

            case BUS, COACH ->
                    "Bus/coach routing is not available because no verified transit routing provider is configured.";

            case METRO,
                 SUBWAY ->
                    "Metro/subway routing is not available because no verified transit routing provider is configured.";

            case TRAM,
                 LIGHT_RAIL ->
                    "Tram/light-rail routing is not available because no verified transit routing provider is configured.";

            case TRAIN,
                 COMMUTER_RAIL ->
                    "Train routing is not available because no verified rail routing provider is configured.";

            case HIGH_SPEED_TRAIN ->
                    "High-speed train routing is not available because no verified rail routing provider is configured.";

            case MONORAIL ->
                    "Monorail routing is not available because no verified transit routing provider is configured.";

            case FLIGHT ->
                    "Flight routing is not available because no verified flight routing provider is configured.";

            case FERRY ->
                    "Ferry routing is not available because no verified water-transit routing provider is configured.";

            case SHIP ->
                    "Ship routing is not available because no verified maritime routing provider is configured.";

            case MULTIMODAL ->
                    "Multimodal routing is not available because no multimodal routing provider is configured.";

            default ->
                    "Map routing is not available for this transport mode.";
        };
    }

    /**
     * Creates a standard unavailable routing response.
     *
     * distance = null
     * duration = null
     * geometry = null
     * alternatives = empty
     * mapAvailable = false
     */
    private RoutingResponse unavailableResponse(
            TransportMode transportMode,
            String message
    ) {

        return new RoutingResponse(
                null,
                null,
                null,
                List.of(),
                false,
                message
        );
    }

    /**
     * Safely converts a numeric response value.
     */
    private double number(
            Object value
    ) {

        if (value instanceof Number number) {

            return number.doubleValue();
        }

        return 0.0;
    }
}