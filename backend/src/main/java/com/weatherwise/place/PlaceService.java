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
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
            ObjectMapper objectMapper,
            @Value("${app.global-transport.osm.overpass-url:https://overpass-api.de/api/interpreter}") String overpassUrl,
            @Value("${app.global-transport.osm.overpass-urls:}") String configuredOverpassUrls
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(900));
        requestFactory.setReadTimeout(Duration.ofMillis(8500));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
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
            key = "T(String).format('%.3f:%.3f:%s:%s:%s', #latitude, #longitude, #category, #radiusKm, #limit)"
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

        // Query two public OSM endpoints concurrently. This avoids serial timeout
        // penalties and lets a healthy mirror answer when the primary is rate-limited.
        List<String> candidates = overpassEndpoints.stream().limit(3).toList();
        List<CompletableFuture<List<PlaceResponse>>> requests = candidates.stream()
                .map(endpoint -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return parsePlaces(requestOverpass(endpoint, query), latitude, longitude, safeLimit);
                    } catch (RestClientException | java.io.IOException ignored) {
                        return List.<PlaceResponse>of();
                    }
                }))
                .toList();

        long deadline = System.nanoTime() + Duration.ofMillis(8800).toNanos();
        List<CompletableFuture<List<PlaceResponse>>> pending = new ArrayList<>(requests);
        while (!pending.isEmpty()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;
            try {
                CompletableFuture.anyOf(pending.toArray(CompletableFuture[]::new)).get(remaining, TimeUnit.NANOSECONDS);
            } catch (Exception timeoutOrFailure) {
                break;
            }
            for (var iterator = pending.iterator(); iterator.hasNext();) {
                CompletableFuture<List<PlaceResponse>> completed = iterator.next();
                if (!completed.isDone()) continue;
                iterator.remove();
                try {
                    List<PlaceResponse> result = completed.getNow(List.of());
                    if (!result.isEmpty()) return result;
                } catch (Exception ignored) {
                    // Another public endpoint may still return usable results.
                }
            }
        }
        return List.of();
    }

    public List<TransportMapPlaceResponse> nearbyTransportPlaces(
            double latitude, double longitude, double radiusKm, int limit, String requestedType
    ) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) return List.of();
        int radiusMeters = (int) Math.round(Math.max(1, Math.min(radiusKm, 35)) * 1000);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String query = """
                [out:json][timeout:8];
                (
                  nwr(around:%d,%f,%f)["railway"~"station|halt|tram_stop|subway_entrance|stop"];
                  nwr(around:%d,%f,%f)["public_transport"~"station|platform|stop_position"];
                  nwr(around:%d,%f,%f)["highway"="bus_stop"];
                  nwr(around:%d,%f,%f)["amenity"~"bus_station|ferry_terminal"];
                  nwr(around:%d,%f,%f)["aeroway"~"aerodrome|terminal"];
                );
                out center tags;
                """.formatted(radiusMeters, latitude, longitude, radiusMeters, latitude, longitude,
                radiusMeters, latitude, longitude, radiusMeters, latitude, longitude, radiusMeters, latitude, longitude);

        List<String> candidates = overpassEndpoints.stream().limit(3).toList();
        List<CompletableFuture<List<TransportMapPlaceResponse>>> requests = candidates.stream()
                .map(endpoint -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return parseTransportPlaces(requestOverpass(endpoint, query), latitude, longitude, safeLimit, requestedType);
                    } catch (RestClientException | java.io.IOException ignored) {
                        return List.<TransportMapPlaceResponse>of();
                    }
                })).toList();
        long deadline = System.nanoTime() + Duration.ofMillis(8800).toNanos();
        List<TransportMapPlaceResponse> combined = new ArrayList<>();
        List<CompletableFuture<List<TransportMapPlaceResponse>>> pending = new ArrayList<>(requests);
        while (!pending.isEmpty()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;
            try {
                CompletableFuture.anyOf(pending.toArray(CompletableFuture[]::new)).get(remaining, TimeUnit.NANOSECONDS);
            } catch (Exception timeoutOrFailure) {
                break;
            }
            for (var iterator = pending.iterator(); iterator.hasNext();) {
                CompletableFuture<List<TransportMapPlaceResponse>> completed = iterator.next();
                if (!completed.isDone()) continue;
                iterator.remove();
                try {
                    for (TransportMapPlaceResponse item : completed.getNow(List.of())) {
                        if (combined.stream().noneMatch(existing -> existing.id().equals(item.id()))) combined.add(item);
                    }
                } catch (Exception ignored) {
                    // Continue collecting from any remaining map-data mirrors.
                }
            }
        }
        return combined.stream()
                .sorted(Comparator.comparing(TransportMapPlaceResponse::distanceKm, Comparator.nullsLast(Double::compareTo)))
                .limit(safeLimit).toList();
    }

    private List<TransportMapPlaceResponse> parseTransportPlaces(
            String body, double originLatitude, double originLongitude, int safeLimit, String requestedType
    ) throws java.io.IOException {
        if (body == null || body.isBlank()) return List.of();
        JsonNode elements = objectMapper.readTree(body).path("elements");
        if (!elements.isArray()) return List.of();
        List<TransportMapPlaceResponse> places = new ArrayList<>();
        for (JsonNode element : elements) {
            JsonNode tags = element.path("tags");
            String name = firstNonBlank(text(tags, "name"), text(tags, "ref"));
            if (name == null || name.isBlank()) continue;
            Double lat = number(element, "lat"), lon = number(element, "lon");
            if (lat == null || lon == null) { lat = number(element.path("center"), "lat"); lon = number(element.path("center"), "lon"); }
            if (lat == null || lon == null) continue;
            String type = classifyTransport(tags);
            if (requestedType != null && !requestedType.isBlank() && !"ALL".equalsIgnoreCase(requestedType)
                    && !type.toLowerCase().contains(requestedType.toLowerCase())) continue;
            places.add(new TransportMapPlaceResponse(
                    element.path("type").asText("node") + ":" + element.path("id").asText(), name, type, lat, lon,
                    firstNonBlank(text(tags, "addr:city"), text(tags, "addr:town"), text(tags, "addr:village")),
                    firstNonBlank(text(tags, "addr:country"), text(tags, "is_in:country")),
                    firstNonBlank(text(tags, "operator"), text(tags, "network")),
                    firstNonBlank(text(tags, "ref"), text(tags, "iata"), text(tags, "uic_ref")),
                    "OpenStreetMap", haversineKm(originLatitude, originLongitude, lat, lon)
            ));
        }
        return places.stream().sorted(Comparator.comparing(TransportMapPlaceResponse::distanceKm, Comparator.nullsLast(Double::compareTo))).limit(safeLimit).toList();
    }

    private String classifyTransport(JsonNode tags) {
        String railway = text(tags, "railway");
        String pt = text(tags, "public_transport");
        String highway = text(tags, "highway");
        String amenity = text(tags, "amenity");
        String aeroway = text(tags, "aeroway");
        if ("aerodrome".equals(aeroway) || "terminal".equals(aeroway)) return "AIRPORT / TERMINAL";
        if ("ferry_terminal".equals(amenity)) return "FERRY TERMINAL";
        if ("bus_station".equals(amenity) || "bus_stop".equals(highway)) return "BUS STATION / STOP";
        if ((railway != null && railway.contains("subway")) || "subway".equals(text(tags, "station"))) return "METRO STATION";
        if ("tram_stop".equals(railway)) return "TRAM STOP";
        if ("halt".equals(railway)) return "RAIL HALT";
        if ("station".equals(railway) || "station".equals(pt)) return "RAIL / TRANSIT STATION";
        if ("platform".equals(pt) || "stop_position".equals(pt) || "stop".equals(railway)) return "TRANSIT STOP / PLATFORM";
        return "TRANSPORT HUB";
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
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["tourism"~"hotel|motel|hostel|guest_house|guesthouse|resort|apartment|attraction|museum|viewpoint|zoo|theme_park"];
                      nwr(around:%d,%f,%f)["leisure"="resort"];
                      nwr(around:%d,%f,%f)["amenity"~"restaurant|fast_food|food_court|cafe"];
                      nwr(around:%d,%f,%f)["leisure"="park"];
                      nwr(around:%d,%f,%f)["historic"];
                    );
                    out center tags;
                    """.formatted(
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude
            );
        }

        return switch (category) {
            case HOTEL -> """
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["tourism"~"hotel|motel"];
                      nwr(around:%d,%f,%f)["building"="hotel"];
                    );
                    out center tags;
                    """.formatted(radiusMeters, latitude, longitude, radiusMeters, latitude, longitude);
            case HOSTEL -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"hostel\"];", radiusMeters, latitude, longitude);
            case GUEST_HOUSE -> one("nwr(around:%d,%f,%f)[\"tourism\"~\"guest_house|guesthouse|apartment\"];", radiusMeters, latitude, longitude);
            case RESORT -> """
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["tourism"="resort"];
                      nwr(around:%d,%f,%f)["leisure"="resort"];
                      nwr(around:%d,%f,%f)["resort"~"yes|hotel|spa|beach|ski"];
                    );
                    out center tags;
                    """.formatted(radiusMeters, latitude, longitude, radiusMeters, latitude, longitude, radiusMeters, latitude, longitude);
            case ATTRACTION -> """
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["tourism"~"attraction|viewpoint|museum|zoo|theme_park"];
                      nwr(around:%d,%f,%f)["historic"];
                      nwr(around:%d,%f,%f)["leisure"="park"];
                      nwr(around:%d,%f,%f)["castle"];
                    );
                    out center tags;
                    """.formatted(
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude,
                    radiusMeters, latitude, longitude
            );
            case MUSEUM -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"museum\"];", radiusMeters, latitude, longitude);
            case VIEWPOINT -> one("nwr(around:%d,%f,%f)[\"tourism\"=\"viewpoint\"];", radiusMeters, latitude, longitude);
            case RESTAURANT -> """
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["amenity"~"restaurant|fast_food|food_court"];
                      nwr(around:%d,%f,%f)["shop"="bakery"];
                    );
                    out center tags;
                    """.formatted(radiusMeters, latitude, longitude, radiusMeters, latitude, longitude);
            case CAFE -> """
                    [out:json][timeout:8];
                    (
                      nwr(around:%d,%f,%f)["amenity"="cafe"];
                      nwr(around:%d,%f,%f)["shop"="coffee"];
                    );
                    out center tags;
                    """.formatted(radiusMeters, latitude, longitude, radiusMeters, latitude, longitude);
            case HOSPITAL -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"hospital\"];", radiusMeters, latitude, longitude);
            case PHARMACY -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"pharmacy\"];", radiusMeters, latitude, longitude);
            case FUEL -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"fuel\"];", radiusMeters, latitude, longitude);
            case EV_CHARGING -> one("nwr(around:%d,%f,%f)[\"amenity\"=\"charging_station\"];", radiusMeters, latitude, longitude);
        };
    }

    private String one(String selector, int radius, double latitude, double longitude) {
        return """
                [out:json][timeout:8];
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
                "OpenStreetMap",
                firstNonBlank(text(tags, "image"), text(tags, "wikimedia_commons")),
                priceInfo(tags)
        );
    }

    private PlaceCategory classify(JsonNode tags) {
        String tourism = text(tags, "tourism");
        String amenity = text(tags, "amenity");
        String resortTag = text(tags, "resort");

        if ("resort".equals(tourism) || "resort".equals(text(tags, "leisure")) || (resortTag != null && !"no".equalsIgnoreCase(resortTag) && !"false".equalsIgnoreCase(resortTag))) return PlaceCategory.RESORT;
        if ("hotel".equals(tourism) || "motel".equals(tourism) || "hotel".equals(text(tags, "building"))) return PlaceCategory.HOTEL;
        if ("hostel".equals(tourism)) return PlaceCategory.HOSTEL;
        if ("guest_house".equals(tourism) || "guesthouse".equals(tourism) || "apartment".equals(tourism)) return PlaceCategory.GUEST_HOUSE;
        if ("attraction".equals(tourism) || "zoo".equals(tourism) || "theme_park".equals(tourism) || "park".equals(text(tags, "leisure")) || tags.has("historic") || tags.has("castle")) return PlaceCategory.ATTRACTION;
        if ("museum".equals(tourism)) return PlaceCategory.MUSEUM;
        if ("viewpoint".equals(tourism)) return PlaceCategory.VIEWPOINT;
        if ("restaurant".equals(amenity) || "fast_food".equals(amenity) || "food_court".equals(amenity) || "bakery".equals(text(tags, "shop"))) return PlaceCategory.RESTAURANT;
        if ("cafe".equals(amenity) || "coffee".equals(text(tags, "shop"))) return PlaceCategory.CAFE;
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

    private String priceInfo(JsonNode tags) {
        String price = firstNonBlank(text(tags, "price"), text(tags, "charge"), text(tags, "fee"));
        if (price == null || price.isBlank()) return null;
        String normalized = price.trim();
        if (normalized.equalsIgnoreCase("yes") || normalized.equalsIgnoreCase("no") || normalized.equalsIgnoreCase("unknown")) return null;
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
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
