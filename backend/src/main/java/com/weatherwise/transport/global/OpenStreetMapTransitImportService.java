package com.weatherwise.transport.global;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Imports public-transport infrastructure from OpenStreetMap through Overpass.
 *
 * This importer is intentionally area-based. It does not query Overpass for
 * every user request. Data is persisted in PostgreSQL and the application
 * searches the local database afterwards.
 */
@Service
public class OpenStreetMapTransitImportService {

    private static final String SOURCE = "OSM";

    private final GlobalTransportPlaceRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String overpassUrl;

    public OpenStreetMapTransitImportService(
            GlobalTransportPlaceRepository repository,
            ObjectMapper objectMapper,
            @Value("${app.global-transport.osm.overpass-url:https://overpass-api.de/api/interpreter}")
            String overpassUrl
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.overpassUrl = overpassUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public int importArea(
            double latitude,
            double longitude,
            double radiusKm
    ) {
        double safeRadius = Math.max(0.5, Math.min(radiusKm, 50.0));

        double latDelta = safeRadius / 111.0;
        double lonDelta = safeRadius /
                (111.0 * Math.max(
                        Math.cos(Math.toRadians(latitude)),
                        0.01
                ));

        double south = latitude - latDelta;
        double north = latitude + latDelta;
        double west = longitude - lonDelta;
        double east = longitude + lonDelta;

        String query = buildOverpassQuery(
                south,
                west,
                north,
                east
        );

        try {
            String encodedQuery =
                    URLEncoder.encode(
                            query,
                            StandardCharsets.UTF_8
                    );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            overpassUrl + "?data=" + encodedQuery
                    ))
                    .timeout(Duration.ofMinutes(3))
                    .header(
                            "User-Agent",
                            "Roamora/1.0 global-transport-import"
                    )
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Overpass returned HTTP " +
                                response.statusCode()
                );
            }

            JsonNode root =
                    objectMapper.readTree(response.body());

            JsonNode elements =
                    root.path("elements");

            if (!elements.isArray()) {
                throw new IllegalStateException(
                        "Overpass response does not contain elements"
                );
            }

            int imported = 0;

            for (JsonNode element : elements) {
                GlobalTransportPlace place =
                        mapElement(element);

                if (place == null) {
                    continue;
                }

                repository.upsert(place);
                imported++;
            }

            return imported;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "OpenStreetMap import interrupted",
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "OpenStreetMap transit import failed",
                    e
            );
        }
    }

    private String buildOverpassQuery(
            double south,
            double west,
            double north,
            double east
    ) {
        String bbox =
                String.format(
                        java.util.Locale.US,
                        "%.7f,%.7f,%.7f,%.7f",
                        south,
                        west,
                        north,
                        east
                );

        /*
         * We intentionally collect infrastructure rather than complete
         * route relations. Route schedules and actual journey planning
         * remain the responsibility of GTFS/other schedule providers.
         */
        return """
                [out:json][timeout:120];

                (
                  node["railway"="station"](%s);
                  node["railway"="halt"](%s);
                  node["railway"="tram_stop"](%s);
                  node["railway"="subway_entrance"](%s);
                  node["public_transport"="station"](%s);
                  node["public_transport"="stop_position"](%s);
                  node["public_transport"="platform"](%s);
                  node["amenity"="bus_station"](%s);
                  node["amenity"="ferry_terminal"](%s);

                  way["railway"="station"](%s);
                  way["railway"="halt"](%s);
                  way["railway"="tram_stop"](%s);
                  way["railway"="subway"](%s);
                  way["public_transport"="station"](%s);
                  way["amenity"="bus_station"](%s);
                  way["amenity"="ferry_terminal"](%s);

                  relation["public_transport"="stop_area"](%s);
                );

                out center tags;
                """.formatted(
                bbox, bbox, bbox, bbox,
                bbox, bbox, bbox,
                bbox, bbox,
                bbox, bbox, bbox, bbox,
                bbox, bbox, bbox,
                bbox
        );
    }

    private GlobalTransportPlace mapElement(JsonNode element) {
        JsonNode tags = element.path("tags");

        if (!tags.isObject()) {
            return null;
        }

        TransportPlaceType type =
                resolveType(tags);

        if (type == null) {
            return null;
        }

        String name =
                text(tags, "name");

        if (name == null || name.isBlank()) {
            /*
             * A nameless platform/entrance is not useful as a searchable
             * public transport place.
             */
            return null;
        }

        double[] coordinates =
                coordinates(element);

        if (coordinates == null) {
            return null;
        }

        String osmType =
                text(element, "type");

        String osmId =
                text(element, "id");

        if (osmType == null || osmId == null) {
            return null;
        }

        String sourceId =
                osmType + "/" + osmId;

        List<TransportMode> modes =
                resolveModes(tags, type);

        String code =
                firstNonBlank(
                        text(tags, "ref"),
                        text(tags, "uic_ref"),
                        text(tags, "local_ref")
                );

        String operator =
                firstNonBlank(
                        text(tags, "operator"),
                        text(tags, "operator:wikidata")
                );

        String parent =
                firstNonBlank(
                        text(tags, "parent_station"),
                        text(tags, "station")
                );

        return new GlobalTransportPlace(
                SOURCE,
                sourceId,
                name,
                type,
                firstNonBlank(
                        text(tags, "addr:city"),
                        text(tags, "is_in:city")
                ),
                firstNonBlank(
                        text(tags, "addr:country"),
                        text(tags, "is_in:country")
                ),
                text(tags, "ISO3166-1"),
                firstNonBlank(
                        text(tags, "addr:state"),
                        text(tags, "is_in:state")
                ),
                coordinates[0],
                coordinates[1],
                text(tags, "timezone"),
                code,
                operator,
                parent,
                null,
                null,
                firstNonBlank(
                        text(tags, "iata"),
                        text(tags, "iata:ref")
                ),
                firstNonBlank(
                        text(tags, "website"),
                        text(tags, "contact:website")
                ),
                modes
        );
    }

    private TransportPlaceType resolveType(JsonNode tags) {
        String railway = text(tags, "railway");
        String amenity = text(tags, "amenity");
        String publicTransport =
                text(tags, "public_transport");

        if ("station".equals(railway)) {
            String station =
                    text(tags, "station");

            if ("subway".equals(station)) {
                return TransportPlaceType.SUBWAY_STATION;
            }

            if ("monorail".equals(station)) {
                return TransportPlaceType.MONORAIL_STATION;
            }

            if ("light_rail".equals(station)) {
                return TransportPlaceType.LIGHT_RAIL_STATION;
            }

            if ("tram".equals(station)) {
                return TransportPlaceType.TRAM_STOP;
            }

            return TransportPlaceType.RAILWAY_STATION;
        }

        if ("halt".equals(railway)) {
            return TransportPlaceType.RAILWAY_STATION;
        }

        if ("tram_stop".equals(railway)) {
            return TransportPlaceType.TRAM_STOP;
        }

        if ("subway_entrance".equals(railway)) {
            return TransportPlaceType.SUBWAY_STATION;
        }

        if ("station".equals(publicTransport)) {
            String mode =
                    text(tags, "train");

            if ("yes".equalsIgnoreCase(mode)) {
                return TransportPlaceType.RAILWAY_STATION;
            }

            String subway =
                    text(tags, "subway");

            if ("yes".equalsIgnoreCase(subway)) {
                return TransportPlaceType.SUBWAY_STATION;
            }

            String tram =
                    text(tags, "tram");

            if ("yes".equalsIgnoreCase(tram)) {
                return TransportPlaceType.TRAM_STOP;
            }

            return TransportPlaceType.RAILWAY_STATION;
        }

        if ("stop_position".equals(publicTransport)
                || "platform".equals(publicTransport)) {

            if ("yes".equalsIgnoreCase(
                    text(tags, "subway")
            )) {
                return TransportPlaceType.SUBWAY_STATION;
            }

            if ("yes".equalsIgnoreCase(
                    text(tags, "tram")
            )) {
                return TransportPlaceType.TRAM_STOP;
            }

            if ("yes".equalsIgnoreCase(
                    text(tags, "train")
            )) {
                return TransportPlaceType.RAILWAY_STATION;
            }

            if ("yes".equalsIgnoreCase(
                    text(tags, "bus")
            )) {
                return TransportPlaceType.BUS_STOP;
            }
        }

        if ("bus_station".equals(amenity)) {
            return TransportPlaceType.BUS_TERMINAL;
        }

        if ("ferry_terminal".equals(amenity)) {
            return TransportPlaceType.FERRY_TERMINAL;
        }

        return null;
    }

    private List<TransportMode> resolveModes(
            JsonNode tags,
            TransportPlaceType type
    ) {
        Set<TransportMode> modes =
                new HashSet<>();

        switch (type) {
            case RAILWAY_STATION -> {
                if ("yes".equalsIgnoreCase(
                        text(tags, "train")
                )) {
                    modes.add(TransportMode.TRAIN);
                } else {
                    modes.add(TransportMode.TRAIN);
                }

                if ("yes".equalsIgnoreCase(
                        text(tags, "commuter_rail")
                )) {
                    modes.add(
                            TransportMode.COMMUTER_RAIL
                    );
                }

                if ("yes".equalsIgnoreCase(
                        text(tags, "highspeed")
                )) {
                    modes.add(
                            TransportMode.HIGH_SPEED_TRAIN
                    );
                }
            }

            case SUBWAY_STATION ->
                    modes.add(TransportMode.SUBWAY);

            case METRO_STATION ->
                    modes.add(TransportMode.METRO);

            case TRAM_STOP ->
                    modes.add(TransportMode.TRAM);

            case LIGHT_RAIL_STATION ->
                    modes.add(TransportMode.LIGHT_RAIL);

            case MONORAIL_STATION ->
                    modes.add(TransportMode.MONORAIL);

            case BUS_STOP, BUS_TERMINAL ->
                    modes.add(TransportMode.BUS);

            case COACH_STOP, COACH_TERMINAL ->
                    modes.add(TransportMode.COACH);

            case FERRY_TERMINAL, PORT -> {
                modes.add(TransportMode.FERRY);
                modes.add(TransportMode.SHIP);
            }

            default -> {
            }
        }

        return List.copyOf(modes);
    }

    private double[] coordinates(JsonNode element) {
        JsonNode lat =
                element.get("lat");

        JsonNode lon =
                element.get("lon");

        if (lat != null && lon != null
                && lat.isNumber() && lon.isNumber()) {
            return new double[]{
                    lat.asDouble(),
                    lon.asDouble()
            };
        }

        JsonNode center =
                element.get("center");

        if (center != null) {
            JsonNode centerLat =
                    center.get("lat");

            JsonNode centerLon =
                    center.get("lon");

            if (centerLat != null
                    && centerLon != null
                    && centerLat.isNumber()
                    && centerLon.isNumber()) {
                return new double[]{
                        centerLat.asDouble(),
                        centerLon.asDouble()
                };
            }
        }

        return null;
    }

    private String text(
            JsonNode node,
            String field
    ) {
        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()
                || !value.isValueNode()) {
            return null;
        }

        String result =
                value.asText();

        return result == null || result.isBlank()
                ? null
                : result.trim();
    }

    private String firstNonBlank(
            String... values
    ) {
        for (String value : values) {
            if (value != null
                    && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }
}
