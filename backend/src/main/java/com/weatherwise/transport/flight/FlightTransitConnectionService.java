package com.weatherwise.transport.flight;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.LocationResponse;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.routing.RoutingOption;
import com.weatherwise.routing.RoutingResponse;
import com.weatherwise.routing.RoutingService;
import com.weatherwise.transport.global.GlobalTransportPlaceService;
import com.weatherwise.transport.gtfs.GtfsJourney;
import com.weatherwise.transport.gtfs.GtfsJourneyService;
import com.weatherwise.transport.gtfs.GtfsSearchRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FlightTransitConnectionService {

    private static final double AIRPORT_SEARCH_RADIUS_KM = 100.0;

    private static final double TRANSIT_SEARCH_RADIUS_KM = 50.0;

    private static final int MAX_AIRPORTS = 10;

    private static final int MAX_TRANSIT_PLACES = 15;

    private static final int MAX_FLIGHT_RESULTS = 20;

    private static final int MAX_JOURNEY_RESULTS = 10;

    private final FlightNetworkService flightNetworkService;

    private final GlobalTransportPlaceService globalTransportPlaceService;

    private final GtfsSearchRepository gtfsSearchRepository;

    private final GtfsJourneyService gtfsJourneyService;

    private final RoutingService routingService;

    private final int airportDepartureBufferMinutes;

    private final int minimumAirportTransitTransferMinutes;

    public FlightTransitConnectionService(
            FlightNetworkService flightNetworkService,
            GlobalTransportPlaceService globalTransportPlaceService,
            GtfsSearchRepository gtfsSearchRepository,
            GtfsJourneyService gtfsJourneyService,
            RoutingService routingService,
            @Value(
                    "${app.flight.minimum-departure-buffer-minutes:120}"
            )
            int airportDepartureBufferMinutes,
            @Value(
                    "${app.flight.minimum-transit-transfer-minutes:20}"
            )
            int minimumAirportTransitTransferMinutes
    ) {

        this.flightNetworkService =
                flightNetworkService;

        this.globalTransportPlaceService =
                globalTransportPlaceService;

        this.gtfsSearchRepository =
                gtfsSearchRepository;

        this.gtfsJourneyService =
                gtfsJourneyService;

        this.routingService =
                routingService;

        this.airportDepartureBufferMinutes =
                Math.max(
                        0,
                        airportDepartureBufferMinutes
                );

        this.minimumAirportTransitTransferMinutes =
                Math.max(
                        0,
                        minimumAirportTransitTransferMinutes
                );
    }

    /**
     * Plans:
     *
     * origin
     *   -> worldwide airport
     *   -> flight
     *   -> worldwide destination airport
     *   -> GTFS transit station
     *   -> GTFS journey
     *   -> destination
     *
     * Flight schedules are obtained only from
     * configured FlightScheduleProvider implementations.
     */
    public FlightTransitConnectionResponse plan(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            LocalDate departureDate,
            TransportMode onwardTransportMode
    ) {
        return plan(originLocation, destinationLocation, departureDate, null, onwardTransportMode);
    }

    public FlightTransitConnectionResponse plan(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            LocalDate departureDate,
            LocalTime departureTime,
            TransportMode onwardTransportMode
    ) {

        validate(
                originLocation,
                destinationLocation,
                departureDate
        );

        TransportMode safeOnwardMode =
                normalizeOnwardMode(
                        onwardTransportMode
                );

        List<TransportPlace> originAirports =
                findAirports(
                        originLocation
                );

        List<TransportPlace> destinationAirports =
                findAirports(
                        destinationLocation
                );

        if (
                originAirports.isEmpty()
                        || destinationAirports.isEmpty()
        ) {

            return FlightTransitConnectionResponse.unavailable(
                    originLocation.getName(),
                    destinationLocation.getName(),
                    safeOnwardMode.name(),
                    "No airport transport places are available near the requested locations."
            );
        }

        List<FlightTransitConnectionOption> options =
                new ArrayList<>();

        for (
                TransportPlace originAirport :
                originAirports
        ) {

            if (!isUsableAirport(originAirport)) {
                continue;
            }

            LocationResponse airportLocation =
                    toLocation(
                            originAirport
                    );

            /*
             * The origin -> airport connector is a real road
             * routing calculation. It is never fabricated.
             */
            RoutingOption originAccess =
                    findConnector(
                            originLocation,
                            airportLocation,
                            TransportMode.CAR
                    );

            if (originAccess == null) {
                continue;
            }

            for (
                    TransportPlace destinationAirport :
                    destinationAirports
            ) {

                if (!isUsableAirport(destinationAirport)) {
                    continue;
                }

                FlightSearchRequest request =
                        new FlightSearchRequest(
                                normalizeCode(
                                        originAirport.code()
                                ),
                                normalizeCode(
                                        destinationAirport.code()
                                ),
                                departureDate,
                                originAirport.id(),
                                destinationAirport.id()
                        );

                FlightSearchResponse flightResponse =
                        flightNetworkService.search(
                                request
                        );

                if (
                        flightResponse == null
                                || flightResponse.flights() == null
                                || flightResponse.flights().isEmpty()
                ) {
                    continue;
                }

                LocationResponse destinationAirportLocation =
                        toLocation(
                                destinationAirport
                        );

                List<TransportPlace> onwardPlaces =
                        findOnwardTransitPlaces(
                                destinationAirport,
                                safeOnwardMode
                        );

                if (onwardPlaces.isEmpty()) {
                    continue;
                }

                for (
                        FlightLeg flight :
                        flightResponse.flights()
                ) {

                    FlightTransitConnectionOption option =
                            buildConnection(
                                    originLocation,
                                    destinationLocation,
                                    originAirport,
                                    destinationAirport,
                                    destinationAirportLocation,
                                    originAccess,
                                    flight,
                                    onwardPlaces,
                                    safeOnwardMode,
                                    departureTime
                            );

                    if (
                            option != null
                                    && option.isAvailable()
                    ) {

                        options.add(option);
                    }
                }
            }
        }

        options.sort(
                Comparator.comparing(
                        option ->
                                option.totalJourneyMinutes() == null
                                        ? Integer.MAX_VALUE
                                        : option.totalJourneyMinutes()
                )
        );

        if (options.size() > MAX_FLIGHT_RESULTS) {

            options =
                    new ArrayList<>(
                            options.subList(
                                    0,
                                    MAX_FLIGHT_RESULTS
                            )
                    );
        }

        if (options.isEmpty()) {

            String message;

            if (
                    !flightNetworkService
                            .hasAvailableProvider()
            ) {

                message =
                        "Live flight schedule data is not configured. "
                                + "No flight route has been fabricated.";

            } else {

                message =
                        "No valid flight-to-transit connection was found "
                                + "with the available flight and transit data.";
            }

            return FlightTransitConnectionResponse.unavailable(
                    originLocation.getName(),
                    destinationLocation.getName(),
                    safeOnwardMode.name(),
                    message
            );
        }

        return FlightTransitConnectionResponse.available(
                originLocation.getName(),
                destinationLocation.getName(),
                safeOnwardMode.name(),
                options
        );
    }

    private FlightTransitConnectionOption buildConnection(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            TransportPlace originAirport,
            TransportPlace destinationAirport,
            LocationResponse destinationAirportLocation,
            RoutingOption originAccess,
            FlightLeg flight,
            List<TransportPlace> onwardPlaces,
            TransportMode onwardTransportMode,
            LocalTime requestedDepartureTime
    ) {

        if (flight == null) {
            return null;
        }

        /*
         * Cancelled flights must never become route options.
         */
        if (flight.status() == FlightStatus.CANCELLED) {
            return null;
        }

        /*
         * Prefer live/estimated departure when available.
         * Otherwise use scheduled departure.
         */
        OffsetDateTime flightDeparture =
                flight.estimatedDeparture() != null
                        ? flight.estimatedDeparture()
                        : flight.scheduledDeparture();

        /*
         * Prefer live/estimated arrival when available.
         * Otherwise use scheduled arrival.
         */
        OffsetDateTime flightArrival =
                flight.estimatedArrival() != null
                        ? flight.estimatedArrival()
                        : flight.scheduledArrival();

        if (
                flightDeparture == null
                        || flightArrival == null
        ) {
            return null;
        }

        if (requestedDepartureTime != null) {
            ZoneId airportZone = resolveZone(originAirport);
            LocalTime actualDepartureTime =
                    flightDeparture.atZoneSameInstant(airportZone).toLocalTime();
            if (actualDepartureTime.isBefore(requestedDepartureTime)) {
                return null;
            }
        }

        if (!flightArrival.isAfter(flightDeparture)) {
            return null;
        }

        int originAccessMinutes =
                safeInt(
                        originAccess.durationMinutes()
                );

        /*
         * The configured buffer is a planning requirement
         * before flight departure.
         *
         * Example:
         *
         * flight departure = 15:00
         * buffer = 120 min
         * required airport arrival = 13:00
         */
        OffsetDateTime requiredAirportArrival =
                flightDeparture.minusMinutes(
                        airportDepartureBufferMinutes
                );

        OffsetDateTime calculatedAirportArrival =
                flightDeparture.minusMinutes(
                        originAccessMinutes
                );

        /*
         * If the origin -> airport journey cannot reach the
         * airport before the required buffer, reject the flight.
         */
        if (
                calculatedAirportArrival
                        .isAfter(
                                requiredAirportArrival
                        )
        ) {

            return null;
        }

        ZoneId airportZone =
                resolveZone(
                        destinationAirport
                );

        LocalDateTime destinationAirportArrivalLocal =
                flightArrival
                        .atZoneSameInstant(
                                airportZone
                        )
                        .toLocalDateTime();

        LocalDate onwardDate =
                destinationAirportArrivalLocal.toLocalDate();

        /*
         * Carry the actual destination TransportPlace together
         * with each journey.
         *
         * This avoids the previous incorrect behaviour where
         * journeyDestinationPlace() performed a second arbitrary
         * nearby search.
         */
        List<OnwardJourneyCandidate> candidates =
                findOnwardJourneys(
                        onwardPlaces,
                        destinationLocation,
                        onwardTransportMode,
                        onwardDate
                );

        for (
                OnwardJourneyCandidate candidate :
                candidates
        ) {

            if (candidate == null) {
                continue;
            }

            TransportPlace onwardPlace =
                    candidate.destinationPlace();

            GtfsJourney journey =
                    candidate.journey();

            if (
                    onwardPlace == null
                            || journey == null
            ) {
                continue;
            }

            if (
                    journey.departureTime() == null
                            || journey.arrivalTime() == null
            ) {
                continue;
            }

            LocalDateTime transitDeparture =
                    journey.departureTime();

            LocalDateTime transitArrival =
                    journey.arrivalTime();

            if (
                    !transitArrival.isAfter(
                            transitDeparture
                    )
            ) {
                continue;
            }

            LocationResponse onwardLocation =
                    toLocation(
                            onwardPlace
                    );

            /*
             * Destination airport -> transit station
             * must be a real route.
             */
            RoutingOption airportToTransit =
                    findConnector(
                            destinationAirportLocation,
                            onwardLocation,
                            TransportMode.CAR
                    );

            if (airportToTransit == null) {
                continue;
            }

            int airportToTransitMinutes =
                    safeInt(
                            airportToTransit.durationMinutes()
                    );

            /*
             * Convert the flight arrival instant into the
             * destination airport's local timezone before
             * comparing it with GTFS local schedule times.
             */
            LocalDateTime earliestTransitDeparture =
                    destinationAirportArrivalLocal
                            .plusMinutes(
                                    airportToTransitMinutes
                                            + minimumAirportTransitTransferMinutes
                            );

            if (
                    transitDeparture.isBefore(
                            earliestTransitDeparture
                    )
            ) {
                continue;
            }

            int flightToTransitWaiting =
                    durationMinutes(
                            destinationAirportArrivalLocal,
                            transitDeparture
                    );

            int transitDuration =
                    durationMinutes(
                            transitDeparture,
                            transitArrival
                    );

            /*
             * The actual GTFS destination stop is used here.
             *
             * No second arbitrary station lookup.
             */
            RoutingOption finalAccess =
                    findConnector(
                            onwardLocation,
                            destinationLocation,
                            TransportMode.CAR
                    );

            /*
             * Never convert an unavailable final connector
             * into 0 minutes.
             */
            if (finalAccess == null) {
                continue;
            }

            int finalJourneyMinutes =
                    safeInt(
                            finalAccess.durationMinutes()
                    );

            int flightDuration =
                    durationMinutes(
                            flightDeparture,
                            flightArrival
                    );

            /*
             * Component duration:
             *
             * origin -> airport
             * + airport departure buffer
             * + flight
             * + airport -> transit waiting
             * + transit journey
             * + final connector
             */
            int totalJourneyMinutes =
                    originAccessMinutes
                            + airportDepartureBufferMinutes
                            + flightDuration
                            + flightToTransitWaiting
                            + transitDuration
                            + finalJourneyMinutes;

            return new FlightTransitConnectionOption(
                    buildOptionId(
                            originAirport,
                            destinationAirport,
                            flight,
                            journey
                    ),
                    originAirport,
                    destinationAirport,
                    flight,
                    onwardPlace,
                    candidate.destinationPlace(),
                    journey,
                    originAccessMinutes,
                    airportDepartureBufferMinutes,
                    airportToTransitMinutes,
                    flightDuration,
                    flightToTransitWaiting,
                    transitDuration,
                    finalJourneyMinutes,
                    totalJourneyMinutes,
                    flightDeparture,
                    flightArrival,
                    "AVAILABLE",
                    "Flight and onward scheduled transit are connected with validated transfer times."
            );
        }

        return null;
    }

    /**
     * Carries the actual GTFS destination place together with
     * its journey.
     */
    private record OnwardJourneyCandidate(
            TransportPlace destinationPlace,
            GtfsJourney journey
    ) {
    }

    private List<OnwardJourneyCandidate> findOnwardJourneys(
            List<TransportPlace> onwardPlaces,
            LocationResponse destinationLocation,
            TransportMode transportMode,
            LocalDate date
    ) {

        if (
                onwardPlaces == null
                        || onwardPlaces.isEmpty()
        ) {
            return List.of();
        }

        TransportPlaceType destinationType =
                resolvePlaceType(
                        transportMode
                );

        if (destinationType == null) {
            return List.of();
        }

        /*
         * Search the destination area only for the requested
         * transit type.
         */
        List<TransportPlace> destinationPlaces =
                gtfsSearchRepository.searchNearby(
                        destinationLocation.getLatitude(),
                        destinationLocation.getLongitude(),
                        destinationType,
                        TRANSIT_SEARCH_RADIUS_KM,
                        MAX_TRANSIT_PLACES
                );

        if (
                destinationPlaces == null
                        || destinationPlaces.isEmpty()
        ) {
            return List.of();
        }

        List<OnwardJourneyCandidate> candidates =
                new ArrayList<>();

        for (
                TransportPlace onwardPlace :
                onwardPlaces
        ) {

            if (
                    onwardPlace == null
                            || onwardPlace.id() == null
                            || onwardPlace.feedId() == null
                            || onwardPlace.feedId().isBlank()
            ) {
                continue;
            }

            for (
                    TransportPlace destinationPlace :
                    destinationPlaces
            ) {

                if (
                        destinationPlace == null
                                || destinationPlace.id() == null
                                || destinationPlace.feedId() == null
                                || destinationPlace.feedId().isBlank()
                ) {
                    continue;
                }

                /*
                 * A GTFS journey belongs to one feed.
                 * Do not attempt to connect stops across
                 * unrelated feeds.
                 */
                if (
                        !onwardPlace.feedId()
                                .equals(
                                        destinationPlace.feedId()
                                )
                ) {
                    continue;
                }

                List<GtfsJourney> journeys =
                        gtfsJourneyService
                                .findJourneysForDate(
                                        onwardPlace.feedId(),
                                        onwardPlace.id(),
                                        destinationPlace.id(),
                                        date,
                                        3,
                                        5,
                                        MAX_JOURNEY_RESULTS
                                );

                if (
                        journeys == null
                                || journeys.isEmpty()
                ) {
                    continue;
                }

                for (
                        GtfsJourney journey :
                        journeys
                ) {

                    if (journey == null) {
                        continue;
                    }

                    /*
                     * Ensure the returned journey really belongs
                     * to the requested station pair.
                     */
                    if (
                            journey.originStopId() != null
                                    && !journey.originStopId()
                                    .equals(
                                            onwardPlace.id()
                                    )
                    ) {
                        continue;
                    }

                    if (
                            journey.destinationStopId() != null
                                    && !journey.destinationStopId()
                                    .equals(
                                            destinationPlace.id()
                                    )
                    ) {
                        continue;
                    }

                    candidates.add(
                            new OnwardJourneyCandidate(
                                    destinationPlace,
                                    journey
                            )
                    );
                }
            }
        }

        candidates.sort(
                Comparator.comparing(
                        candidate ->
                                candidate.journey() == null
                                        ? null
                                        : candidate.journey().departureTime(),
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                )
        );

        if (
                candidates.size()
                        <= MAX_JOURNEY_RESULTS
        ) {
            return List.copyOf(
                    candidates
            );
        }

        return List.copyOf(
                candidates.subList(
                        0,
                        MAX_JOURNEY_RESULTS
                )
        );
    }

    /**
     * Worldwide airport lookup.
     *
     * Airports must come from the global transport-place
     * layer, not GTFS.
     */
    private List<TransportPlace> findAirports(
            LocationResponse location
    ) {

        if (location == null) {
            return List.of();
        }

        List<TransportPlace> airports =
                globalTransportPlaceService.nearby(
                        location.getLatitude(),
                        location.getLongitude(),
                        TransportPlaceType.AIRPORT,
                        AIRPORT_SEARCH_RADIUS_KM,
                        MAX_AIRPORTS
                );

        if (
                airports == null
                        || airports.isEmpty()
        ) {
            return List.of();
        }

        return airports.stream()
                .filter(this::isUsableAirport)
                .limit(MAX_AIRPORTS)
                .toList();
    }

    private boolean isUsableAirport(
            TransportPlace airport
    ) {

        if (airport == null) {
            return false;
        }

        if (
                airport.type()
                        != TransportPlaceType.AIRPORT
        ) {
            return false;
        }

        if (
                airport.latitude() == null
                        || airport.longitude() == null
        ) {
            return false;
        }

        /*
         * Either an IATA/ICAO/code or the provider's global
         * airport ID must be available because the flight
         * provider may use either identifier.
         */
        return (
                airport.code() != null
                        && !airport.code().isBlank()
        )
                || (
                airport.id() != null
                        && !airport.id().isBlank()
        );
    }

    private List<TransportPlace> findOnwardTransitPlaces(
            TransportPlace airport,
            TransportMode transportMode
    ) {

        TransportPlaceType type =
                resolvePlaceType(
                        transportMode
                );

        if (type == null) {
            return List.of();
        }

        if (
                airport == null
                        || airport.latitude() == null
                        || airport.longitude() == null
        ) {
            return List.of();
        }

        List<TransportPlace> places =
                gtfsSearchRepository.searchNearby(
                        airport.latitude(),
                        airport.longitude(),
                        type,
                        TRANSIT_SEARCH_RADIUS_KM,
                        MAX_TRANSIT_PLACES
                );

        return places == null
                ? List.of()
                : places;
    }

    /**
     * Real routing connector.
     *
     * Only supported road modes reach the routing provider.
     * If routing is unavailable, the connector is rejected.
     */
    private RoutingOption findConnector(
            LocationResponse origin,
            LocationResponse destination,
            TransportMode mode
    ) {

        if (
                origin == null
                        || destination == null
        ) {
            return null;
        }

        try {

            RoutingResponse response =
                    routingService.calculateRoute(
                            origin,
                            destination,
                            mode
                    );

            if (
                    response == null
                            || response.alternatives() == null
                            || response.alternatives().isEmpty()
            ) {
                return null;
            }

            return response.alternatives()
                    .stream()
                    .filter(
                            option ->
                                    option != null
                                            && option.durationMinutes() != null
                            )
                    .min(
                            Comparator.comparing(
                                    RoutingOption::durationMinutes
                            )
                    )
                    .orElse(null);

        } catch (Exception ignored) {

            return null;
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

    private ZoneId resolveZone(
            TransportPlace place
    ) {

        if (
                place != null
                        && place.timezone() != null
                        && !place.timezone().isBlank()
        ) {

            try {

                return ZoneId.of(
                        place.timezone()
                );

            } catch (Exception ignored) {
                // Fall through to UTC.
            }
        }

        return ZoneId.of(
                "UTC"
        );
    }

    private TransportMode normalizeOnwardMode(
            TransportMode mode
    ) {

        if (
                mode == null
                        || mode == TransportMode.FLIGHT
                        || mode == TransportMode.MULTIMODAL
                        || mode == TransportMode.CAR
                        || mode == TransportMode.MOTORCYCLE
                        || mode == TransportMode.TAXI
                        || mode == TransportMode.BICYCLE
                        || mode == TransportMode.SCOOTER
                        || mode == TransportMode.WALKING
        ) {

            return TransportMode.TRAIN;
        }

        return mode;
    }

    private TransportPlaceType resolvePlaceType(
            TransportMode mode
    ) {

        if (mode == null) {
            return null;
        }

        return switch (mode) {

            case BUS ->
                    TransportPlaceType.BUS_STOP;

            case COACH ->
                    TransportPlaceType.COACH_STOP;

            case METRO ->
                    TransportPlaceType.METRO_STATION;

            case SUBWAY ->
                    TransportPlaceType.SUBWAY_STATION;

            case TRAM ->
                    TransportPlaceType.TRAM_STOP;

            case LIGHT_RAIL ->
                    TransportPlaceType.LIGHT_RAIL_STATION;

            case MONORAIL ->
                    TransportPlaceType.MONORAIL_STATION;

            case TRAIN,
                 COMMUTER_RAIL,
                 HIGH_SPEED_TRAIN ->
                    TransportPlaceType.RAILWAY_STATION;

            case FERRY,
                 SHIP ->
                    TransportPlaceType.FERRY_TERMINAL;

            default ->
                    null;
        };
    }

    private String normalizeCode(
            String code
    ) {

        if (
                code == null
                        || code.isBlank()
        ) {
            return null;
        }

        return code.trim();
    }

    private int durationMinutes(
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (
                start == null
                        || end == null
        ) {
            return 0;
        }

        long minutes =
                Duration.between(
                        start,
                        end
                ).toMinutes();

        return (int) Math.max(
                0,
                Math.min(
                        Integer.MAX_VALUE,
                        minutes
                )
        );
    }

    private int durationMinutes(
            OffsetDateTime start,
            OffsetDateTime end
    ) {

        if (
                start == null
                        || end == null
        ) {
            return 0;
        }

        long minutes =
                Duration.between(
                        start,
                        end
                ).toMinutes();

        return (int) Math.max(
                0,
                Math.min(
                        Integer.MAX_VALUE,
                        minutes
                )
        );
    }

    private int safeInt(
            Integer value
    ) {

        return value == null
                ? 0
                : Math.max(
                        0,
                        value
                );
    }

    private String buildOptionId(
            TransportPlace originAirport,
            TransportPlace destinationAirport,
            FlightLeg flight,
            GtfsJourney journey
    ) {

        return "FLIGHT:"
                + safe(
                        originAirport == null
                                ? null
                                : originAirport.id()
                )
                + ":"
                + safe(
                        destinationAirport == null
                                ? null
                                : destinationAirport.id()
                )
                + ":"
                + safe(
                        flight == null
                                ? null
                                : flight.flightId()
                )
                + ":"
                + safe(
                        journey == null
                                ? null
                                : journey.feedId()
                )
                + ":"
                + safe(
                        journey == null
                                ? null
                                : journey.tripId()
                );
    }

    private String safe(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return "UNKNOWN";
        }

        return value;
    }

    private void validate(
            LocationResponse origin,
            LocationResponse destination,
            LocalDate departureDate
    ) {

        if (origin == null) {

            throw new IllegalArgumentException(
                    "Origin location is required."
            );
        }

        if (destination == null) {

            throw new IllegalArgumentException(
                    "Destination location is required."
            );
        }

        if (departureDate == null) {

            throw new IllegalArgumentException(
                    "Departure date is required."
            );
        }
    }
}