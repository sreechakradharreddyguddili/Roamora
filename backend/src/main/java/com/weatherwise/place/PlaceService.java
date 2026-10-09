package com.weatherwise.place;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class PlaceService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final List<String> overpassEndpoints;

    public PlaceService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.global-transport.osm.overpass-url:https://overpass-api.de/api/interpreter}") String overpassUrl,
            @Value("${app.global-transport.osm.overpass-urls:}") String configuredOverpassUrls
    ) {
        this.restClient = restClientBuilder
                .defaultHeader("User-Agent", "Roamora-Journey-Intelligence/1.0")
                .build();
        this.objectMapper = objectMapper;

        List<String> configured = Arrays.stream(configuredOverpassUrls.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        List<String> endpoints = new ArrayList<>();
        if (!configured.isEmpty()) {
            endpoints.addAll(configured);
        }
        addIfMissing(endpoints, overpassUrl);
        addIfMissing(endpoints, "https://overpass.kumi.systems/api/interpreter");
        addIfMissing(endpoints, "https://overpass.private.coffee/api/interpreter");
        this.overpassEndpoints = List.copyOf(endpoints);
    }

    @Cacheable(
            value = "nearbyPlaces",
            key = "T(String).format('%.3f:%.3f:%s:%s:%s', #latitude, #longitude, #category, #radiusKm, #limit)",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<PlaceResponse> nearby(
            double latitude,
            double longitude,
            PlaceCategory category,
            double radiusKm,
            int limit
    ) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return List.of();
        }

        double safeRadius = Math.max(0.5, Math.min(radiusKm, 25.0));
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String query = buildQuery(latitude, longitude, safeRadius, category);

        for (String endpoint : overpassEndpoints) {
            try {
                String body = requestOverpass(endpoint, query);
                List<PlaceResponse> places = parsePlaces(body, latitude, longitude, safeLimit);
                if (!places.isEmpty()) {
                    return places;
                }
            } catch (RestClientException | java.io.IOException ignored) {
                // Try the next public Overpass instance.
            }
        }

        return List.of();
    }

    private String requestOverpass(String endpoint, String query) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
    }

    private List<PlaceResponse> parsePlaces(
            String body,
            double originLatitude,
            double originLongitude,
            int safeLimit
    ) throws java.io.IOException {
        if (body == null || body.isBlank()) return List.of();

        JsonNode elements = objectMapper.readTree(body).path("elements");
        if (!elements.isArray()) return List.of();

        List<PlaceResponse> places = new ArrayList<>();
        for (JsonNode element : elements) {
            PlaceResponse place = toPlace(element, originLatitude, originLongitude);
            if (place != null) places.add(place);
        }

        return places.stream()
                .sorted(Comparator.comparing(
                        PlaceResponse::distanceKm,
                        Comparator.nullsLast(Double::compareTo)
                ))
                .limit(safeLimit)
                .toList();
    }

    private String buildQuery(double latitude, double longitude, double radiusKm, PlaceCategory category) {
        int radiusMeters = (int) Math.round(radiusKm * 1000);

        if (category == null) {
            return """
                    [out:json][timeout:25];
                    (
                      nwr(around:%d,%f,%f)["tourism"~"hotel|hostel|guest_house|resort|attraction|museum|viewpoint"];
                      nwr(around:%d,%f,%f)["amenity"~"restaurant|cafe"];
                    );
                    out center tags;
                    """.formatted(
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude
            );
        }

        return switch (category) {
            case HOTEL -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"hotel\"];", radiusMeters, latitude, longitude);
            case HOSTEL -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"hostel\"];", radiusMeters, latitude, longitude);
            case GUEST_HOUSE -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"guest_house\"];", radiusMeters, latitude, longitude);
            case RESORT -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"resort\"];", radiusMeters, latitude, longitude);
            case ATTRACTION -> """
                    [out:json][timeout:25];
                    (
                      nwr(around:%d,%f,%f)["tourism"="attraction"];
                      nwr(around:%d,%f,%f)["tourism"="viewpoint"];
                      nwr(around:%d,%f,%f)["historic"];
                    );
                    out center tags;
                    """.formatted(
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude
            );
            case MUSEUM -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"museum\"];", radiusMeters, latitude, longitude);
            case VIEWPOINT -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"viewpoint\"];", radiusMeters, latitude, longitude);
            case RESTAURANT -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"restaurant\"];", radiusMeters, latitude, longitude);
            case CAFE -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"cafe\"];", radiusMeters, latitude, longitude);
            case HOSPITAL -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"hospital\"];", radiusMeters, latitude, longitude);
            case PHARMACY -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"pharmacy\"];", radiusMeters, latitude, longitude);
            case FUEL -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"fuel\"];", radiusMeters, latitude, longitude);
            case EV_CHARGING -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"charging_station\"];", radiusMeters, latitude, longitude);
        };
    }

    private String one(String selector, int radius, double latitude, double longitude) {
        return """
                [out:json][timeout:25];
                (
                  %s
                );
                out center tags;
                """.formatted(selector.formatted(radius, latitude, longitude));
    }

    private PlaceResponse toPlace(JsonNode element, double originLatitude, double originLongitude) {
        JsonNode tags = element.path("tags");
        String name = text(tags, "name");
        if (name == null || name.isBlank()) return null;

        Double latitude = number(element, "lat");
        Double longitude = number(element, "lon");
        if (latitude == null || longitude == null) {
            JsonNode center = element.path("center");
            latitude = number(center, "lat");
            longitude = number(center, "lon");
        }
        if (latitude == null || longitude == null) return null;

        PlaceCategory category = classify(tags);
        if (category == null) return null;

        return new PlaceResponse(
                element.path("type").asText("node") + ":" + element.path("id").asText(),
                name,
                category,
                latitude,
                longitude,
                buildAddress(tags),
                firstNonBlank(text(tags, "website"), text(tags, "contact:website")),
                firstNonBlank(text(tags, "phone"), text(tags, "contact:phone")),
                haversineKm(originLatitude, originLongitude, latitude, longitude),
                parseStarRating(text(tags, "stars")),
                "OpenStreetMap"
        );
    }

    private PlaceCategory classify(JsonNode tags) {
        String tourism = text(tags, "tourism");
        String amenity = text(tags, "amenity");

        if ("hotel".equals(tourism)) return PlaceCategory.HOTEL;
        if ("hostel".equals(tourism)) return PlaceCategory.HOSTEL;
        if ("guest_house".equals(tourism)) return PlaceCategory.GUEST_HOUSE;
        if ("resort".equals(tourism)) return PlaceCategory.RESORT;
        if ("attraction".equals(tourism)) return PlaceCategory.ATTRACTION;
        if ("museum".equals(tourism)) return PlaceCategory.MUSEUM;
        if ("viewpoint".equals(tourism)) return PlaceCategory.VIEWPOINT;
        if ("restaurant".equals(amenity)) return PlaceCategory.RESTAURANT;
        if ("cafe".equals(amenity)) return PlaceCategory.CAFE;
        if ("hospital".equals(amenity)) return PlaceCategory.HOSPITAL;
        if ("pharmacy".equals(amenity)) return PlaceCategory.PHARMACY;
        if ("fuel".equals(amenity)) return PlaceCategory.FUEL;
        if ("charging_station".equals(amenity)) return PlaceCategory.EV_CHARGING;
        if (tags.has("historic")) return PlaceCategory.ATTRACTION;
        return null;
    }

    private String buildAddress(JsonNode tags) {
        String street = text(tags, "addr:street");
        String house = text(tags, "addr:housenumber");
        String city = firstNonBlank(text(tags, "addr:city"), text(tags, "addr:town"), text(tags, "addr:village"));
        String postcode = text(tags, "addr:postcode");

        List<String> parts = new ArrayList<>();
        if (house != null && street != null) parts.add(house + " " + street);
        else if (street != null) parts.add(street);
        if (city != null) parts.add(city);
        if (postcode != null) parts.add(postcode);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private Double parseStarRating(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            double rating = Double.parseDouble(value.trim().replace(',', '.'));
            if (!Double.isFinite(rating) || rating < 1 || rating > 5) return null;
            return rating;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void addIfMissing(List<String> endpoints, String endpoint) {
        if (endpoint != null && !endpoint.isBlank() && !endpoints.contains(endpoint)) {
            endpoints.add(endpoint);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
