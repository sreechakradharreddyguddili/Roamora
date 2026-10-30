package com.weatherwise.transport.flight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aviationstack implementation of the provider-neutral flight schedule contract.
 *
 * The provider is deliberately disabled when no API key is configured.
 * This keeps the application honest: no flight is fabricated when live
 * aviation data is unavailable.
 *
 * Configuration:
 *
 * app.flight.aviationstack.api-key=${AVIATIONSTACK_API_KEY:}
 * app.flight.aviationstack.base-url=https://api.aviationstack.com/v1
 */
@Component
public class AviationstackFlightScheduleProvider
        implements FlightScheduleProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AviationstackFlightScheduleProvider(
            ObjectMapper objectMapper,
            @Value("${app.flight.aviationstack.api-key:}") String apiKey,
            @Value(
                    "${app.flight.aviationstack.base-url:https://api.aviationstack.com/v1}"
            )
            String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = normalize(apiKey);

        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .build();
    }

    @Override
    public String getProviderName() {
        return "Aviationstack";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public List<FlightLeg> search(
            FlightSearchRequest request
    ) {
        if (!isAvailable()) {
            return List.of();
        }

        if (request == null) {
            return List.of();
        }

        /*
         * Aviationstack identifies airport searches primarily through
         * IATA codes. Prefer those when available.
         */
        String originCode =
                normalize(request.originAirportCode());

        String destinationCode =
                normalize(request.destinationAirportCode());

        if (originCode == null || destinationCode == null) {
            return List.of();
        }

        LocalDate departureDate =
                request.departureDate();

        if (departureDate == null) {
            return List.of();
        }

        try {
            JsonNode response =
                    restClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path("/flights")
                                            .queryParam(
                                                    "access_key",
                                                    apiKey
                                            )
                                            .queryParam(
                                                    "dep_iata",
                                                    originCode
                                            )
                                            .queryParam(
                                                    "arr_iata",
                                                    destinationCode
                                            )
                                            .queryParam(
                                                    "flight_date",
                                                    departureDate.toString()
                                            )
                                            .build()
                            )
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(JsonNode.class);

            return mapResponse(
                    response,
                    request
            );

        } catch (Exception ignored) {
            /*
             * Provider failures must not become fabricated routes.
             * FlightNetworkService will continue to the next provider
             * or return an unavailable response.
             */
            return List.of();
        }
    }

    private List<FlightLeg> mapResponse(
            JsonNode response,
            FlightSearchRequest request
    ) {
        if (response == null) {
            return List.of();
        }

        JsonNode error =
                response.path("error");

        if (!error.isMissingNode()
                && !error.isNull()) {
            return List.of();
        }

        JsonNode data =
                response.path("data");

        if (!data.isArray()
                || data.isEmpty()) {
            return List.of();
        }

        List<FlightLeg> flights =
                new ArrayList<>();

        for (JsonNode item : data) {
            FlightLeg flight =
                    mapFlight(item, request);

            if (flight != null) {
                flights.add(flight);
            }
        }

        return List.copyOf(flights);
    }

    private FlightLeg mapFlight(
            JsonNode item,
            FlightSearchRequest request
    ) {
        if (item == null || item.isNull()) {
            return null;
        }

        JsonNode departure =
                item.path("departure");

        JsonNode arrival =
                item.path("arrival");

        JsonNode flight =
                item.path("flight");

        JsonNode airline =
                item.path("airline");

        JsonNode aircraft =
                item.path("aircraft");

        String originCode =
                firstNonBlank(
                        text(departure, "iata"),
                        request.originAirportCode()
                );

        String destinationCode =
                firstNonBlank(
                        text(arrival, "iata"),
                        request.destinationAirportCode()
                );

        if (originCode == null
                || destinationCode == null) {
            return null;
        }

        String flightNumber =
                firstNonBlank(
                        text(flight, "iata"),
                        text(flight, "icao"),
                        text(flight, "number")
                );

        if (flightNumber == null) {
            return null;
        }

        String flightId =
                firstNonBlank(
                        text(flight, "iata"),
                        text(flight, "icao"),
                        flightNumber
                );

        String airlineCode =
                firstNonBlank(
                        text(airline, "iata"),
                        text(airline, "icao")
                );

        String airlineName =
                text(airline, "name");

        ZoneId departureZone =
                resolveZone(
                        text(departure, "timezone")
                );

        ZoneId arrivalZone =
                resolveZone(
                        text(arrival, "timezone")
                );

        OffsetDateTime scheduledDeparture =
                parseDateTime(
                        text(departure, "scheduled"),
                        departureZone
                );

        OffsetDateTime scheduledArrival =
                parseDateTime(
                        text(arrival, "scheduled"),
                        arrivalZone
                );

        OffsetDateTime estimatedDeparture =
                parseDateTime(
                        firstNonBlank(
                                text(departure, "estimated"),
                                text(departure, "actual")
                        ),
                        departureZone
                );

        OffsetDateTime estimatedArrival =
                parseDateTime(
                        firstNonBlank(
                                text(arrival, "estimated"),
                                text(arrival, "actual")
                        ),
                        arrivalZone
                );

        /*
         * Do not turn a flight with no usable schedule into a route.
         */
        if (scheduledDeparture == null
                || scheduledArrival == null) {
            return null;
        }

        Integer departureDelay =
                integer(
                        departure,
                        "delay"
                );

        Integer arrivalDelay =
                integer(
                        arrival,
                        "delay"
                );

        Integer delayMinutes =
                maxNullable(
                        departureDelay,
                        arrivalDelay
                );

        FlightStatus status =
                resolveStatus(
                        text(item, "flight_status"),
                        scheduledDeparture,
                        scheduledArrival,
                        estimatedDeparture,
                        estimatedArrival,
                        delayMinutes
                );

        String aircraftType =
                firstNonBlank(
                        text(aircraft, "iata"),
                        text(aircraft, "icao"),
                        text(aircraft, "registration")
                );

        String terminal =
                firstNonBlank(
                        text(departure, "terminal"),
                        text(arrival, "terminal")
                );

        String gate =
                firstNonBlank(
                        text(departure, "gate"),
                        text(arrival, "gate")
                );

        return new FlightLeg(
                getProviderName(),
                flightId,
                airlineCode,
                airlineName,
                flightNumber,
                firstNonBlank(
                        request.originAirportId(),
                        originCode
                ),
                originCode,
                text(departure, "airport"),
                firstNonBlank(
                        request.destinationAirportId(),
                        destinationCode
                ),
                destinationCode,
                text(arrival, "airport"),
                scheduledDeparture,
                scheduledArrival,
                estimatedDeparture,
                estimatedArrival,
                delayMinutes,
                calculateDuration(
                        scheduledDeparture,
                        scheduledArrival
                ),
                status,
                aircraftType,
                terminal,
                gate,
                true
        );
    }

    private FlightStatus resolveStatus(
            String providerStatus,
            OffsetDateTime scheduledDeparture,
            OffsetDateTime scheduledArrival,
            OffsetDateTime estimatedDeparture,
            OffsetDateTime estimatedArrival,
            Integer delayMinutes
    ) {
        String status =
                normalize(providerStatus);

        if (status != null) {
            switch (status.toLowerCase()) {
                case "cancelled":
                case "canceled":
                    return FlightStatus.CANCELLED;

                case "active":
                case "en-route":
                case "enroute":
                    return FlightStatus.IN_AIR;

                case "landed":
                    return FlightStatus.LANDED;

                case "scheduled":
                    if (delayMinutes != null
                            && delayMinutes > 0) {
                        return FlightStatus.DELAYED;
                    }
                    return FlightStatus.SCHEDULED;

                case "incident":
                case "diverted":
                    return FlightStatus.DELAYED;

                default:
                    break;
            }
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        if (estimatedArrival != null
                && estimatedArrival.isBefore(now)) {
            return FlightStatus.LANDED;
        }

        if (estimatedDeparture != null
                && estimatedDeparture.isBefore(now)
                && scheduledArrival.isAfter(now)) {
            return FlightStatus.IN_AIR;
        }

        if (delayMinutes != null
                && delayMinutes > 0) {
            return FlightStatus.DELAYED;
        }

        if (scheduledDeparture.isAfter(now)) {
            return FlightStatus.SCHEDULED;
        }

        if (scheduledArrival.isAfter(now)) {
            return FlightStatus.IN_AIR;
        }

        return FlightStatus.UNKNOWN;
    }

    private OffsetDateTime parseDateTime(
            String value,
            ZoneId zone
    ) {
        String normalized =
                normalize(value);

        if (normalized == null) {
            return null;
        }

        /*
         * First support values that already contain an offset.
         */
        try {
            return OffsetDateTime.parse(
                    normalized
            );
        } catch (DateTimeParseException ignored) {
        }

        /*
         * Aviationstack commonly returns local airport time
         * without an explicit offset. Apply the airport timezone
         * supplied by the provider.
         */
        try {
            LocalDateTime local =
                    LocalDateTime.parse(
                            normalized,
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );

            return local.atZone(zone)
                    .toOffsetDateTime();

        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private ZoneId resolveZone(
            String timezone
    ) {
        String normalized =
                normalize(timezone);

        if (normalized != null) {
            try {
                return ZoneId.of(
                        normalized
                );
            } catch (Exception ignored) {
            }
        }

        return ZoneOffset.UTC;
    }

    private java.time.Duration calculateDuration(
            OffsetDateTime departure,
            OffsetDateTime arrival
    ) {
        if (departure == null
                || arrival == null
                || !arrival.isAfter(departure)) {
            return null;
        }

        return java.time.Duration.between(
                departure,
                arrival
        );
    }

    private Integer integer(
            JsonNode node,
            String field
    ) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()
                || !node.has(field)
                || node.get(field).isNull()) {
            return null;
        }

        try {
            return node.get(field).asInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer maxNullable(
            Integer first,
            Integer second
    ) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return Math.max(
                first,
                second
        );
    }

    private String text(
            JsonNode node,
            String field
    ) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        JsonNode value =
                node.get(field);

        if (value == null
                || value.isNull()) {
            return null;
        }

        return normalize(
                value.asText()
        );
    }

    private String firstNonBlank(
            String... values
    ) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            String normalized =
                    normalize(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String normalize(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeBaseUrl(
            String value
    ) {
        String normalized =
                normalize(value);

        if (normalized == null) {
            return "https://api.aviationstack.com/v1";
        }

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }
}
