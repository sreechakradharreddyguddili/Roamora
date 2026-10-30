package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.LocationResponse;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.routing.RoutingOption;
import com.weatherwise.routing.RoutingResponse;
import com.weatherwise.routing.RoutingService;
import com.weatherwise.transport.gtfs.GtfsJourney;
import com.weatherwise.transport.gtfs.GtfsJourneyService;
import com.weatherwise.transport.gtfs.GtfsSearchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MultimodalTransitPlannerService {

    private static final double PLACE_SEARCH_RADIUS_KM = 25.0;
    private static final int PLACE_LIMIT = 25;
    private static final int JOURNEY_LIMIT = 5;
    private static final int MAX_OPTIONS = 20;

    private static final List<TransportMode> TRANSIT_MODES = List.of(
            TransportMode.BUS,
            TransportMode.COACH,
            TransportMode.METRO,
            TransportMode.SUBWAY,
            TransportMode.TRAM,
            TransportMode.LIGHT_RAIL,
            TransportMode.MONORAIL,
            TransportMode.TRAIN,
            TransportMode.COMMUTER_RAIL,
            TransportMode.HIGH_SPEED_TRAIN,
            TransportMode.FERRY,
            TransportMode.SHIP
    );

    private final GtfsSearchRepository gtfsSearchRepository;
    private final GtfsJourneyService journeyService;
    private final RoutingService routingService;

    public MultimodalTransitPlannerService(
            GtfsSearchRepository gtfsSearchRepository,
            GtfsJourneyService journeyService,
            RoutingService routingService
    ) {
        this.gtfsSearchRepository = gtfsSearchRepository;
        this.journeyService = journeyService;
        this.routingService = routingService;
    }

    /**
     * Plans multimodal ground journeys using:
     *
     * origin
     *   -> real walking connector
     *   -> real GTFS boarding place
     *   -> real scheduled GTFS journey
     *   -> real walking connector
     *   -> destination
     *
     * The GTFS journey itself may contain transfers between
     * supported transit modes because GtfsJourneyService already
     * provides the graph-based multi-leg journey layer.
     */
    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination
    ) {
        return plan(origin, destination, LocalDate.now(), null);
    }

    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination,
            LocalDate departureDate
    ) {
        return plan(origin, destination, departureDate, null);
    }

    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination,
            LocalDate departureDate,
            LocalTime departureTime
    ) {
        if (origin == null || destination == null) {
            return unavailable("Origin and destination are required.");
        }

        Map<String, MultimodalTransitOption> uniqueOptions =
                new LinkedHashMap<>();

        for (TransportMode mode : TRANSIT_MODES) {
            if (uniqueOptions.size() >= MAX_OPTIONS) {
                break;
            }

            addOptionsForMode(
                    uniqueOptions,
                    origin,
                    destination,
                    mode,
                    departureDate,
                    departureTime
            );
        }

        List<MultimodalTransitOption> options =
                new ArrayList<>(uniqueOptions.values());

        options.sort(
                Comparator.comparing(
                        option -> option.totalKnownDurationMinutes() == null
                                ? Integer.MAX_VALUE
                                : option.totalKnownDurationMinutes()
                )
        );

        if (options.size() > MAX_OPTIONS) {
            options = new ArrayList<>(
                    options.subList(0, MAX_OPTIONS)
            );
        }

        if (options.isEmpty()) {
            return unavailable(
                    "No multimodal journey with real walking connectors and verified scheduled transit data was found."
            );
        }

        return new MultimodalTransitPlanResponse(
                "AVAILABLE",
                "Multimodal journeys found using real first/last-mile and scheduled transit data.",
                List.copyOf(options)
        );
    }

    /**
     * Backward-compatible entry point for callers that previously
     * supplied one requested transit mode.
     */
    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination,
            TransportMode requestedMode
    ) {
        return plan(
                origin,
                destination,
                requestedMode,
                LocalDate.now(),
                null
        );
    }

    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination,
            TransportMode requestedMode,
            LocalDate departureDate
    ) {
        return plan(origin, destination, requestedMode, departureDate, null);
    }

    public MultimodalTransitPlanResponse plan(
            LocationResponse origin,
            LocationResponse destination,
            TransportMode requestedMode,
            LocalDate departureDate,
            LocalTime departureTime
    ) {
        if (requestedMode == null || requestedMode == TransportMode.MULTIMODAL) {
            return plan(origin, destination, departureDate, departureTime);
        }

        TransportPlaceType placeType =
                resolvePlaceType(requestedMode);

        if (placeType == null) {
            return unavailable(
                    "Multimodal scheduled planning is not available for "
                            + requestedMode + "."
            );
        }

        Map<String, MultimodalTransitOption> uniqueOptions =
                new LinkedHashMap<>();

        addOptionsForMode(
                uniqueOptions,
                origin,
                destination,
                requestedMode,
                departureDate,
                departureTime
        );

        List<MultimodalTransitOption> options =
                new ArrayList<>(uniqueOptions.values());

        options.sort(
                Comparator.comparing(
                        option -> option.totalKnownDurationMinutes() == null
                                ? Integer.MAX_VALUE
                                : option.totalKnownDurationMinutes()
                )
        );

        if (options.isEmpty()) {
            return unavailable(
                    "No multimodal journey with real connectors and scheduled "
                            + requestedMode + " data was found."
            );
        }

        return new MultimodalTransitPlanResponse(
                "AVAILABLE",
                "Multimodal journeys found.",
                List.copyOf(options)
        );
    }

    private void addOptionsForMode(
            Map<String, MultimodalTransitOption> uniqueOptions,
            LocationResponse origin,
            LocationResponse destination,
            TransportMode mode,
            LocalDate departureDate,
            LocalTime departureTime
    ) {
        TransportPlaceType placeType =
                resolvePlaceType(mode);

        if (placeType == null) {
            return;
        }

        List<TransportPlace> originPlaces =
                gtfsSearchRepository.searchNearby(
                        origin.getLatitude(),
                        origin.getLongitude(),
                        placeType,
                        PLACE_SEARCH_RADIUS_KM,
                        PLACE_LIMIT
                );

        List<TransportPlace> destinationPlaces =
                gtfsSearchRepository.searchNearby(
                        destination.getLatitude(),
                        destination.getLongitude(),
                        placeType,
                        PLACE_SEARCH_RADIUS_KM,
                        PLACE_LIMIT
                );

        if (originPlaces.isEmpty() || destinationPlaces.isEmpty()) {
            return;
        }

        Map<String, List<TransportPlace>> destinationsByFeed =
                groupByFeed(destinationPlaces);

        LocalDate serviceDate =
                resolveServiceDate(originPlaces, departureDate);

        for (TransportPlace boarding : originPlaces) {
            if (uniqueOptions.size() >= MAX_OPTIONS) {
                return;
            }

            if (boarding == null
                    || boarding.feedId() == null
                    || boarding.feedId().isBlank()) {
                continue;
            }

            List<TransportPlace> candidates =
                    destinationsByFeed.get(boarding.feedId());

            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            MultimodalConnector access =
                    buildWalkingConnector(origin, boarding);

            if (!"AVAILABLE".equals(access.status())) {
                continue;
            }

            for (TransportPlace alighting : candidates) {
                if (uniqueOptions.size() >= MAX_OPTIONS) {
                    return;
                }

                if (alighting == null) {
                    continue;
                }

                List<GtfsJourney> journeys =
                        journeyService.findJourneysForDate(
                                boarding.feedId(),
                                boarding.id(),
                                alighting.id(),
                                serviceDate,
                                6,
                                5,
                                JOURNEY_LIMIT
                        );

                for (GtfsJourney journey : journeys) {
                    if (departureTime != null
                            && journey != null
                            && journey.departureTime() != null
                            && journey.departureTime().toLocalTime().isBefore(departureTime)) {
                        continue;
                    }
                    if (uniqueOptions.size() >= MAX_OPTIONS) {
                        return;
                    }

                    if (!isUsableJourney(journey)) {
                        continue;
                    }

                    MultimodalConnector egress =
                            buildWalkingConnector(
                                    toLocation(alighting),
                                    destination
                            );

                    if (!"AVAILABLE".equals(egress.status())) {
                        continue;
                    }

                    Double totalDistance =
                            sumDistances(
                                    access.distanceKm(),
                                    egress.distanceKm()
                            );

                    Integer totalDuration =
                            sumDurations(
                                    access.durationMinutes(),
                                    journey.durationMinutes(),
                                    egress.durationMinutes()
                            );

                    String optionId =
                            buildOptionId(
                                    mode,
                                    journey,
                                    boarding,
                                    alighting
                            );

                    uniqueOptions.putIfAbsent(
                            optionId,
                            new MultimodalTransitOption(
                                    optionId,
                                    boarding,
                                    alighting,
                                    access,
                                    journey,
                                    egress,
                                    totalDistance,
                                    totalDuration,
                                    "AVAILABLE"
                            )
                    );
                }
            }
        }
    }

    private boolean isUsableJourney(GtfsJourney journey) {
        if (journey == null
                || journey.departureTime() == null
                || journey.arrivalTime() == null) {
            return false;
        }

        return journey.arrivalTime()
                .isAfter(journey.departureTime());
    }

    private MultimodalConnector buildWalkingConnector(
            LocationResponse from,
            TransportPlace to
    ) {
        if (to == null
                || to.latitude() == null
                || to.longitude() == null) {
            return unavailableConnector(
                    from == null ? null : from.getName(),
                    to == null ? null : to.name(),
                    "Transport place has no coordinates."
            );
        }

        return buildWalkingConnector(
                from,
                toLocation(to)
        );
    }

    private MultimodalConnector buildWalkingConnector(
            LocationResponse from,
            LocationResponse to
    ) {
        if (from == null || to == null) {
            return unavailableConnector(
                    from == null ? null : from.getName(),
                    to == null ? null : to.getName(),
                    "Walking connector locations are unavailable."
            );
        }

        try {
            RoutingResponse response =
                    routingService.calculateRoute(
                            from,
                            to,
                            TransportMode.WALKING
                    );

            if (response == null
                    || response.alternatives() == null
                    || response.alternatives().isEmpty()) {
                return unavailableConnector(
                        from.getName(),
                        to.getName(),
                        "Walking connector is unavailable."
                );
            }

            RoutingOption option =
                    response.alternatives().get(0);

            if (option == null
                    || option.geometry() == null
                    || option.geometry().isBlank()
                    || option.durationMinutes() < 0) {
                return unavailableConnector(
                        from.getName(),
                        to.getName(),
                        "Walking connector did not return usable mapped geometry."
                );
            }

            return new MultimodalConnector(
                    TransportMode.WALKING,
                    "AVAILABLE",
                    option.distanceKm(),
                    option.durationMinutes(),
                    from.getName(),
                    to.getName(),
                    null
            );
        } catch (Exception ignored) {
            return unavailableConnector(
                    from.getName(),
                    to.getName(),
                    "Walking connector is unavailable."
            );
        }
    }

    private LocationResponse toLocation(
            TransportPlace place
    ) {
        return new LocationResponse(
                place.name(),
                place.country(),
                place.latitude(),
                place.longitude()
        );
    }

    private Map<String, List<TransportPlace>> groupByFeed(
            List<TransportPlace> places
    ) {
        Map<String, List<TransportPlace>> grouped =
                new HashMap<>();

        for (TransportPlace place : places) {
            if (place == null
                    || place.feedId() == null
                    || place.feedId().isBlank()) {
                continue;
            }

            grouped.computeIfAbsent(
                    place.feedId(),
                    ignored -> new ArrayList<>()
            ).add(place);
        }

        return grouped;
    }

    private TransportPlaceType resolvePlaceType(
            TransportMode mode
    ) {
        if (mode == null) {
            return null;
        }

        return switch (mode) {
            case BUS -> TransportPlaceType.BUS_STOP;
            case COACH -> TransportPlaceType.COACH_STOP;
            case METRO -> TransportPlaceType.METRO_STATION;
            case SUBWAY -> TransportPlaceType.SUBWAY_STATION;
            case TRAM -> TransportPlaceType.TRAM_STOP;
            case LIGHT_RAIL -> TransportPlaceType.LIGHT_RAIL_STATION;
            case MONORAIL -> TransportPlaceType.MONORAIL_STATION;
            case TRAIN, COMMUTER_RAIL, HIGH_SPEED_TRAIN ->
                    TransportPlaceType.RAILWAY_STATION;
            case FERRY, SHIP -> TransportPlaceType.FERRY_TERMINAL;
            default -> null;
        };
    }

    private LocalDate resolveServiceDate(
            List<TransportPlace> places,
            LocalDate requestedDate
    ) {
        if (requestedDate != null) {
            return requestedDate;
        }

        for (TransportPlace place : places) {
            String timezone = place.timezone();

            if (timezone == null || timezone.isBlank()) {
                continue;
            }

            try {
                return LocalDate.now(ZoneId.of(timezone));
            } catch (Exception ignored) {
            }
        }

        return LocalDate.now();
    }

    private Double sumDistances(
            Double access,
            Double egress
    ) {
        if (access == null || egress == null) {
            return null;
        }

        return round(access + egress);
    }

    private Integer sumDurations(
            Integer access,
            double transitMinutes,
            Integer egress
    ) {
        if (access == null || egress == null) {
            return null;
        }

        return access
                + (int) Math.max(0, Math.round(transitMinutes))
                + egress;
    }

    private String buildOptionId(
            TransportMode mode,
            GtfsJourney journey,
            TransportPlace boarding,
            TransportPlace alighting
    ) {
        return "MULTIMODAL:"
                + safe(mode == null ? null : mode.name())
                + ":"
                + safe(journey.feedId())
                + ":"
                + safe(journey.tripId())
                + ":"
                + safe(boarding.id())
                + ":"
                + safe(alighting.id());
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "UNKNOWN"
                : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private MultimodalConnector unavailableConnector(
            String from,
            String to,
            String message
    ) {
        return new MultimodalConnector(
                TransportMode.WALKING,
                "UNAVAILABLE",
                null,
                null,
                from,
                to,
                message
        );
    }

    private MultimodalTransitPlanResponse unavailable(
            String message
    ) {
        return new MultimodalTransitPlanResponse(
                "UNAVAILABLE",
                message,
                List.of()
        );
    }
}
