package com.weatherwise.cache;

import com.weatherwise.dto.RouteOptionResponse;
import com.weatherwise.enums.RiskLevel;
import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.LocationResponse;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.routing.RoutingOption;
import com.weatherwise.routing.RoutingResponse;
import com.weatherwise.routing.RoutingService;
import com.weatherwise.service.RouteRecommendationService;
import com.weatherwise.service.RouteRiskService;
import com.weatherwise.transport.gtfs.GtfsJourney;
import com.weatherwise.transport.gtfs.GtfsJourneyLeg;
import com.weatherwise.transport.gtfs.GtfsJourneyService;
import com.weatherwise.transport.flight.FlightTransitConnectionOption;
import com.weatherwise.transport.flight.FlightTransitConnectionResponse;
import com.weatherwise.transport.flight.FlightTransitConnectionService;
import com.weatherwise.transport.gtfs.GtfsSearchRepository;
import com.weatherwise.transport.global.MultimodalTransitOption;
import com.weatherwise.transport.global.MultimodalTransitPlanResponse;
import com.weatherwise.transport.global.MultimodalTransitPlannerService;
import com.weatherwise.weather.RouteWeatherPoint;
import com.weatherwise.weather.RouteWeatherService;
import com.weatherwise.weather.WeatherResponse;
import com.weatherwise.weather.WeatherService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoutePlanCacheService {

    private final RoutingService routingService;
    private final WeatherService weatherService;
    private final RouteWeatherService routeWeatherService;
    private final RouteRiskService routeRiskService;
    private final RouteRecommendationService routeRecommendationService;
    private final GtfsSearchRepository gtfsSearchRepository;
    private final GtfsJourneyService gtfsJourneyService;
    private final FlightTransitConnectionService flightTransitConnectionService;
    private final MultimodalTransitPlannerService multimodalTransitPlannerService;

    public RoutePlanCacheService(
            RoutingService routingService,
            WeatherService weatherService,
            RouteWeatherService routeWeatherService,
            RouteRiskService routeRiskService,
            RouteRecommendationService routeRecommendationService,
            GtfsSearchRepository gtfsSearchRepository,
            GtfsJourneyService gtfsJourneyService,
            FlightTransitConnectionService flightTransitConnectionService,
            MultimodalTransitPlannerService multimodalTransitPlannerService
    ) {

        this.routingService =
                routingService;

        this.weatherService =
                weatherService;

        this.routeWeatherService =
                routeWeatherService;

        this.routeRiskService =
                routeRiskService;

        this.routeRecommendationService =
                routeRecommendationService;

        this.gtfsSearchRepository =
                gtfsSearchRepository;

        this.gtfsJourneyService =
                gtfsJourneyService;

        this.flightTransitConnectionService =
                flightTransitConnectionService;

        this.multimodalTransitPlannerService =
                multimodalTransitPlannerService;
    }

    public RoutePlanCache calculate(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            TransportMode transportMode
    ) {
        return calculate(
                originLocation,
                destinationLocation,
                transportMode,
                LocalDate.now(),
                null
        );
    }

    public RoutePlanCache calculate(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            TransportMode transportMode,
            LocalDate departureDate
    ) {
        return calculate(originLocation, destinationLocation, transportMode, departureDate, null);
    }

    @Cacheable(
            value = "routePlans",
            key = """
                    #originLocation.latitude + ':' +
                    #originLocation.longitude + ':' +
                    #destinationLocation.latitude + ':' +
                    #destinationLocation.longitude + ':' +
                    #transportMode + ':' +
                    #departureDate + ':' +
                    #departureTime
                    """,
            unless = "#result == null"
    )
    public RoutePlanCache calculate(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            TransportMode transportMode,
            LocalDate departureDate,
            LocalTime departureTime
    ) {

        RoutingResponse routingResponse =
                routingService.calculateRoute(
                        originLocation,
                        destinationLocation,
                        transportMode
                );

        WeatherResponse originWeather =
                weatherService.getWeather(
                        originLocation.getLatitude(),
                        originLocation.getLongitude()
                );

        WeatherResponse destinationWeather =
                weatherService.getWeather(
                        destinationLocation.getLatitude(),
                        destinationLocation.getLongitude()
                );

        /*
         * Normal mapped route.
         *
         * Road-based modes continue to use
         * the existing RoutingService.
         */
        if (
                routingResponse.alternatives() != null
                        && !routingResponse.alternatives().isEmpty()
        ) {

            List<RouteOptionResponse> alternatives =
                    buildAlternativeRoutes(
                            routingResponse,
                            originWeather,
                            destinationWeather,
                            transportMode
                    );

            RouteOptionResponse bestRoute =
                    routeRecommendationService.selectBestRoute(
                            alternatives
                    );

            if (bestRoute == null) {

                throw new IllegalStateException(
                        "Unable to determine the best mapped route."
                );
            }

            markRecommendedRoute(
                    alternatives,
                    bestRoute
            );

            return new RoutePlanCache(
                    routingResponse,
                    originWeather,
                    destinationWeather,
                    alternatives,
                    bestRoute
            );
        }

        /*
         * No map route.
         *
         * MULTIMODAL routes use the dedicated multimodal planner.
         * The planner combines real first/last-mile walking connectors
         * with verified GTFS journeys. It never replaces missing
         * transit data with a CAR route.
         */
        if (transportMode == TransportMode.MULTIMODAL) {

            List<RouteOptionResponse> multimodalRoutes =
                    buildMultimodalRoutes(
                            originLocation,
                            destinationLocation,
                            originWeather,
                            destinationWeather,
                            departureDate,
                            departureTime
                    );

            if (!multimodalRoutes.isEmpty()) {

                calculateCombinedScores(
                        multimodalRoutes
                );

                RouteOptionResponse bestRoute =
                        routeRecommendationService.selectBestRoute(
                                multimodalRoutes
                        );

                if (bestRoute != null) {

                    markRecommendedRoute(
                            multimodalRoutes,
                            bestRoute
                    );

                    return new RoutePlanCache(
                            routingResponse,
                            originWeather,
                            destinationWeather,
                            multimodalRoutes,
                            bestRoute
                    );
                }
            }
        }

        /*
         * No map route.
         *
         * Flight routes use the dedicated flight-to-transit
         * connection layer. A flight is never replaced by a
         * road route when flight data is unavailable.
         */
        if (transportMode == TransportMode.FLIGHT) {

            List<RouteOptionResponse> flightRoutes =
                    buildFlightConnectionRoutes(
                            originLocation,
                            destinationLocation,
                            originWeather,
                            destinationWeather,
                            departureDate,
                            departureTime
                    );

            if (!flightRoutes.isEmpty()) {

                calculateCombinedScores(
                        flightRoutes
                );

                RouteOptionResponse bestRoute =
                        routeRecommendationService.selectBestRoute(
                                flightRoutes
                        );

                if (bestRoute != null) {

                    markRecommendedRoute(
                            flightRoutes,
                            bestRoute
                    );

                    return new RoutePlanCache(
                            routingResponse,
                            originWeather,
                            destinationWeather,
                            flightRoutes,
                            bestRoute
                    );
                }
            }
        }

        /*
         * No map route.
         *
         * For scheduled/public transport we now try
         * the GTFS journey layer before declaring the
         * route unavailable.
         */
        if (isGtfsMode(transportMode)) {

            List<RouteOptionResponse> journeyRoutes =
                    buildGtfsJourneyRoutes(
                            originLocation,
                            destinationLocation,
                            originWeather,
                            destinationWeather,
                            transportMode,
                            departureDate
                    );

            if (!journeyRoutes.isEmpty()) {

                calculateCombinedScores(
                        journeyRoutes
                );

                RouteOptionResponse bestRoute =
                        routeRecommendationService.selectBestRoute(
                                journeyRoutes
                        );

                if (bestRoute != null) {

                    markRecommendedRoute(
                            journeyRoutes,
                            bestRoute
                    );

                    return new RoutePlanCache(
                            routingResponse,
                            originWeather,
                            destinationWeather,
                            journeyRoutes,
                            bestRoute
                    );
                }
            }
        }

        /*
         * No mapped route and no scheduled
         * transport journey available.
         */
        RouteOptionResponse unavailableRoute =
                buildUnavailableRoute(
                        originWeather,
                        destinationWeather,
                        transportMode,
                        routingResponse.availabilityMessage()
                );

        List<RouteOptionResponse> alternatives =
                new ArrayList<>();

        alternatives.add(
                unavailableRoute
        );

        return new RoutePlanCache(
                routingResponse,
                originWeather,
                destinationWeather,
                alternatives,
                unavailableRoute
        );
    }

    private List<RouteOptionResponse> buildAlternativeRoutes(
            RoutingResponse routingResponse,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            TransportMode transportMode
    ) {

        List<RouteOptionResponse> routes =
                new ArrayList<>();

        List<RoutingOption> options =
                routingResponse.alternatives();

        if (
                options == null
                        || options.isEmpty()
        ) {
            return routes;
        }

        for (RoutingOption option : options) {

            List<RouteWeatherPoint> routeWeather =
                    routeWeatherService.getWeatherAlongRoute(
                            option.geometry()
                    );

            RiskLevel riskLevel =
                    routeRiskService.calculateRisk(
                            originWeather,
                            destinationWeather,
                            routeWeather,
                            transportMode
                    );

            int riskScore =
                    routeRiskService.calculateRiskScore(
                            originWeather,
                            destinationWeather,
                            routeWeather,
                            transportMode
                    );

            String recommendation =
                    routeRiskService.getRecommendation(
                            riskLevel
                    );

            routes.add(
                    new RouteOptionResponse(
                            option.routeKey(),
                            option.distanceKm(),
                            option.durationMinutes(),
                            riskScore,
                            riskLevel,
                            recommendation,
                            false,
                            option.geometry(),
                            routeWeather
                    )
            );
        }

        calculateCombinedScores(
                routes
        );

        return routes;
    }

    /*
     * ============================================================
     * FLIGHT CONNECTION INTEGRATION
     * ============================================================
     */

    private List<RouteOptionResponse> buildMultimodalRoutes(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            LocalDate departureDate,
            LocalTime departureTime
    ) {

        MultimodalTransitPlanResponse response =
                multimodalTransitPlannerService.plan(
                        originLocation,
                        destinationLocation,
                        departureDate,
                        departureTime
                );

        if (response == null
                || response.options() == null
                || response.options().isEmpty()) {
            return List.of();
        }

        List<RouteOptionResponse> routes =
                new ArrayList<>();

        for (MultimodalTransitOption option : response.options()) {

            if (option == null
                    || !"AVAILABLE".equalsIgnoreCase(option.availability())) {
                continue;
            }

            RouteOptionResponse route =
                    buildMultimodalRoute(
                            option,
                            originWeather,
                            destinationWeather
                    );

            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    private RouteOptionResponse buildMultimodalRoute(
            MultimodalTransitOption option,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather
    ) {

        List<RouteWeatherPoint> emptyWeather =
                List.of();

        TransportMode riskMode =
                resolveMultimodalRiskMode(option);

        RiskLevel riskLevel =
                routeRiskService.calculateRisk(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        riskMode
                );

        int riskScore =
                routeRiskService.calculateRiskScore(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        riskMode
                );

        String recommendation =
                routeRiskService.getRecommendation(
                        riskLevel
                );

        RouteOptionResponse route =
                new RouteOptionResponse(
                        option.optionId(),
                        option.totalKnownDistanceKm(),
                        option.totalKnownDurationMinutes(),
                        riskScore,
                        riskLevel,
                        recommendation,
                        false,
                        null,
                        emptyWeather
                );

        route.setMultimodalOption(option);
        route.setMultimodalStatus("AVAILABLE");
        route.setMultimodalMessage(
                "Multimodal journey assembled from real walking connectors and scheduled transit data."
        );

        route.setJourney(option.journey());

        return route;
    }

    private TransportMode resolveMultimodalRiskMode(
            MultimodalTransitOption option
    ) {

        if (option == null || option.journey() == null
                || option.journey().legs() == null
                || option.journey().legs().isEmpty()) {
            return TransportMode.MULTIMODAL;
        }

        GtfsJourneyLeg firstLeg =
                option.journey().legs().get(0);

        if (firstLeg == null || firstLeg.routeType() == null) {
            return TransportMode.MULTIMODAL;
        }

        return switch (firstLeg.routeType()) {
            case "0", "5" -> TransportMode.TRAM;
            case "1" -> TransportMode.SUBWAY;
            case "2", "7" -> TransportMode.TRAIN;
            case "3", "11" -> TransportMode.BUS;
            case "4" -> TransportMode.FERRY;
            case "6", "12" -> TransportMode.MONORAIL;
            default -> TransportMode.MULTIMODAL;
        };
    }

    private List<RouteOptionResponse> buildFlightConnectionRoutes(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            LocalDate departureDate,
            LocalTime departureTime
    ) {

        /*
         * The flight connection layer defaults its onward
         * scheduled transport to TRAIN when no explicit
         * onward mode is supplied.
         *
         * This call is intentionally separate from RoutingService.
         * OSRM cannot calculate an actual flight route and must
         * never be used as a flight fallback.
         */
        FlightTransitConnectionResponse response =
                flightTransitConnectionService.plan(
                        originLocation,
                        destinationLocation,
                        resolveFlightDepartureDate(originLocation, departureDate),
                        departureTime,
                        TransportMode.TRAIN
                );

        if (response == null
                || response.options() == null
                || response.options().isEmpty()) {
            return List.of();
        }

        List<RouteOptionResponse> routes =
                new ArrayList<>();

        for (FlightTransitConnectionOption option :
                response.options()) {

            if (option == null || !option.isAvailable()) {
                continue;
            }

            RouteOptionResponse route =
                    buildFlightConnectionRoute(
                            option,
                            originWeather,
                            destinationWeather
                    );

            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    private RouteOptionResponse buildFlightConnectionRoute(
            FlightTransitConnectionOption option,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather
    ) {

        List<RouteWeatherPoint> emptyWeather =
                List.of();

        RiskLevel riskLevel =
                routeRiskService.calculateRisk(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        TransportMode.FLIGHT
                );

        int riskScore =
                routeRiskService.calculateRiskScore(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        TransportMode.FLIGHT
                );

        String recommendation =
                routeRiskService.getRecommendation(
                        riskLevel
                );

        Integer durationMinutes =
                option.totalJourneyMinutes();

        RouteOptionResponse route =
                new RouteOptionResponse(
                        option.optionId(),
                        null,
                        durationMinutes,
                        riskScore,
                        riskLevel,
                        recommendation,
                        false,
                        null,
                        emptyWeather
                );

        route.setOriginAirport(
                option.originAirport()
        );

        route.setDestinationAirport(
                option.destinationAirport()
        );

        route.setFlight(
                option.flight()
        );

        route.setOnwardBoardingPlace(
                option.onwardPlace()
        );

        route.setOnwardAlightingPlace(
                option.journeyDestinationPlace()
        );

        route.setJourney(
                option.journey()
        );

        route.setOriginAccessMinutes(
                option.originAccessMinutes()
        );

        route.setAirportDepartureBufferMinutes(
                option.airportDepartureBufferMinutes()
        );

        route.setAirportToTransitMinutes(
                option.airportToTransitMinutes()
        );

        route.setFlightDurationMinutes(
                option.flightDurationMinutes()
        );

        route.setFlightToTransitWaitingMinutes(
                option.flightToTransitWaitingMinutes()
        );

        route.setTransitDurationMinutes(
                option.transitDurationMinutes()
        );

        route.setFinalJourneyMinutes(
                option.finalJourneyMinutes()
        );

        route.setTotalJourneyMinutes(
                option.totalJourneyMinutes()
        );

        route.setFlightDeparture(
                option.flightDeparture()
        );

        route.setFlightArrival(
                option.flightArrival()
        );

        route.setFlightConnectionStatus(
                option.status()
        );

        route.setFlightConnectionMessage(
                option.message()
        );

        return route;
    }

    private LocalDate resolveFlightDepartureDate(
            LocationResponse originLocation,
            LocalDate requestedDate
    ) {

        return requestedDate == null
                ? LocalDate.now()
                : requestedDate;
    }

    /*
     * ============================================================
     * GTFS JOURNEY INTEGRATION
     * ============================================================
     */

    private List<RouteOptionResponse> buildGtfsJourneyRoutes(
            LocationResponse originLocation,
            LocationResponse destinationLocation,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            TransportMode transportMode,
            LocalDate departureDate
    ) {

        TransportPlaceType placeType =
                resolveGtfsPlaceType(
                        transportMode
                );

        if (placeType == null) {
            return List.of();
        }

        /*
         * Find real GTFS stops near the requested
         * origin and destination coordinates.
         */
        List<TransportPlace> originPlaces =
                gtfsSearchRepository.searchNearby(
                        originLocation.getLatitude(),
                        originLocation.getLongitude(),
                        placeType,
                        25.0,
                        25
                );

        List<TransportPlace> destinationPlaces =
                gtfsSearchRepository.searchNearby(
                        destinationLocation.getLatitude(),
                        destinationLocation.getLongitude(),
                        placeType,
                        25.0,
                        25
                );

        if (
                originPlaces.isEmpty()
                        || destinationPlaces.isEmpty()
        ) {
            return List.of();
        }

        /*
         * Group destination stops by feed.
         *
         * A stop ID is only unique inside a GTFS feed.
         */
        Map<String, List<TransportPlace>> destinationsByFeed =
                new HashMap<>();

        for (TransportPlace destinationPlace :
                destinationPlaces) {

            if (
                    destinationPlace.feedId() == null
                            || destinationPlace.feedId().isBlank()
            ) {
                continue;
            }

            destinationsByFeed
                    .computeIfAbsent(
                            destinationPlace.feedId(),
                            key -> new ArrayList<>()
                    )
                    .add(destinationPlace);
        }

        if (destinationsByFeed.isEmpty()) {
            return List.of();
        }

        List<RouteOptionResponse> routes =
                new ArrayList<>();

        /*
         * Use the origin stop timezone when available.
         * Otherwise use the system date.
         */
        LocalDate serviceDate =
                resolveServiceDate(
                        originPlaces,
                        departureDate
                );

        for (TransportPlace originPlace :
                originPlaces) {

            String feedId =
                    originPlace.feedId();

            if (
                    feedId == null
                            || feedId.isBlank()
            ) {
                continue;
            }

            List<TransportPlace> destinationCandidates =
                    destinationsByFeed.get(
                            feedId
                    );

            if (
                    destinationCandidates == null
                            || destinationCandidates.isEmpty()
            ) {
                continue;
            }

            for (TransportPlace destinationPlace :
                    destinationCandidates) {

                List<GtfsJourney> journeys =
                        gtfsJourneyService.findJourneysForDate(
                                feedId,
                                originPlace.id(),
                                destinationPlace.id(),
                                serviceDate,
                                10
                        );

                if (journeys.isEmpty()) {
                    continue;
                }

                for (GtfsJourney journey :
                        journeys) {

                    RouteOptionResponse route =
                            buildJourneyRoute(
                                    journey,
                                    originPlace,
                                    destinationPlace,
                                    originWeather,
                                    destinationWeather,
                                    transportMode,
                                    serviceDate
                            );

                    routes.add(route);
                }

                /*
                 * Keep the number of generated options
                 * bounded.
                 */
                if (routes.size() >= 20) {
                    return routes
                            .stream()
                            .limit(20)
                            .toList();
                }
            }
        }

        return routes;
    }

    private RouteOptionResponse buildJourneyRoute(
            GtfsJourney journey,
            TransportPlace originPlace,
            TransportPlace destinationPlace,
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            TransportMode transportMode,
            LocalDate serviceDate
    ) {

        int durationMinutes =
                (int) Math.max(
                        0,
                        journey.durationMinutes()
                );

        double distanceKm =
                calculateDistanceKm(
                        originPlace.latitude(),
                        originPlace.longitude(),
                        destinationPlace.latitude(),
                        destinationPlace.longitude()
                );

        List<RouteWeatherPoint> emptyWeather =
                List.of();

        RiskLevel riskLevel =
                routeRiskService.calculateRisk(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        transportMode
                );

        int riskScore =
                routeRiskService.calculateRiskScore(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        transportMode
                );

        String recommendation =
                routeRiskService.getRecommendation(
                        riskLevel
                );

        GtfsJourney datedJourney =
                rebaseJourneyToDate(
                        journey,
                        serviceDate
                );

        String routeKey =
                buildJourneyRouteKey(
                        datedJourney
                );

        RouteOptionResponse route =
                new RouteOptionResponse(
                        routeKey,
                        distanceKm,
                        durationMinutes,
                        riskScore,
                        riskLevel,
                        recommendation,
                        false,
                        null,
                        emptyWeather
                );

        route.setJourney(
                datedJourney
        );

        return route;
    }

    private GtfsJourney rebaseJourneyToDate(
            GtfsJourney journey,
            LocalDate serviceDate
    ) {

        if (journey == null || serviceDate == null) {
            return journey;
        }

        java.time.LocalDateTime departure =
                rebaseDate(journey.departureTime(), serviceDate);

        java.time.LocalDateTime arrival =
                rebaseDate(journey.arrivalTime(), serviceDate);

        List<GtfsJourneyLeg> rebasedLegs = new ArrayList<>();

        if (journey.legs() != null) {
            for (GtfsJourneyLeg leg : journey.legs()) {
                if (leg == null) {
                    continue;
                }

                java.time.LocalDateTime legDeparture =
                        rebaseDate(leg.departureTime(), serviceDate);

                java.time.LocalDateTime legArrival =
                        rebaseDate(leg.arrivalTime(), serviceDate);

                rebasedLegs.add(new GtfsJourneyLeg(
                        leg.feedId(),
                        leg.originStopId(),
                        leg.originStopName(),
                        leg.destinationStopId(),
                        leg.destinationStopName(),
                        leg.tripId(),
                        leg.routeId(),
                        leg.routeShortName(),
                        leg.routeLongName(),
                        leg.routeType(),
                        leg.serviceId(),
                        legDeparture,
                        legArrival,
                        leg.travelDuration(),
                        leg.stopCount(),
                        leg.destination(),
                        leg.status(),
                        leg.estimatedDepartureTime(),
                        leg.estimatedArrivalTime(),
                        leg.delaySeconds(),
                        leg.realtimeAvailable()
                ));
            }
        }

        return new GtfsJourney(
                journey.feedId(),
                journey.originStopId(),
                journey.originStopName(),
                journey.destinationStopId(),
                journey.destinationStopName(),
                journey.tripId(),
                journey.routeId(),
                journey.routeShortName(),
                journey.routeLongName(),
                journey.routeType(),
                journey.serviceId(),
                departure,
                arrival,
                journey.journeyDuration(),
                journey.transfers(),
                journey.destination(),
                journey.status(),
                List.copyOf(rebasedLegs),
                journey.waitingTime(),
                journey.transferTime(),
                journey.estimatedDepartureTime(),
                journey.estimatedArrivalTime(),
                journey.delaySeconds(),
                journey.realtimeAvailable(),
                journey.vehicles(),
                journey.alerts()
        );
    }

    private java.time.LocalDateTime rebaseDate(
            java.time.LocalDateTime value,
            LocalDate date
    ) {

        if (value == null) {
            return null;
        }

        long dayOffset =
                Duration.between(
                        java.time.LocalDate.of(
                                1970,
                                1,
                                1
                        ).atStartOfDay(),
                        value
                ).toDays();

        java.time.LocalDate actualDate =
                date.plusDays(dayOffset);

        return java.time.LocalDateTime.of(
                actualDate,
                value.toLocalTime()
        );
    }

    private String buildJourneyRouteKey(
            GtfsJourney journey
    ) {

        StringBuilder key = new StringBuilder("GTFS:");

        key.append(safe(journey.feedId()));
        key.append(":");
        key.append(safe(journey.originStopId()));
        key.append(":");
        key.append(safe(journey.destinationStopId()));
        key.append(":");
        key.append(journey.departureTime());
        key.append(":");
        key.append(journey.arrivalTime());

        if (journey.legs() != null) {
            for (GtfsJourneyLeg leg : journey.legs()) {
                if (leg == null) {
                    continue;
                }
                key.append(":");
                key.append(safe(leg.tripId()));
                key.append(":");
                key.append(safe(leg.originStopId()));
                key.append(":");
                key.append(safe(leg.destinationStopId()));
            }
        }

        return key.toString();
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value;
    }

    /*
     * Map application transport modes to GTFS
     * stop categories.
     */
    private TransportPlaceType resolveGtfsPlaceType(
            TransportMode transportMode
    ) {

        if (transportMode == null) {
            return null;
        }

        return switch (transportMode) {

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

    private boolean isGtfsMode(
            TransportMode transportMode
    ) {

        return resolveGtfsPlaceType(
                transportMode
        ) != null;
    }

    private LocalDate resolveServiceDate(
            List<TransportPlace> places,
            LocalDate requestedDate
    ) {

        if (requestedDate != null) {
            return requestedDate;
        }

        for (TransportPlace place : places) {

            String timezone =
                    place.timezone();

            if (
                    timezone == null
                            || timezone.isBlank()
            ) {
                continue;
            }

            try {

                return LocalDate.now(
                        ZoneId.of(timezone)
                );

            } catch (Exception ignored) {
                // Continue to the next place.
            }
        }

        return LocalDate.now();
    }

    /*
     * Straight-line distance between the two
     * selected GTFS stops.
     *
     * This is NOT presented as the actual transit
     * route distance.
     */
    private double calculateDistanceKm(
            Double latitude1,
            Double longitude1,
            Double latitude2,
            Double longitude2
    ) {

        if (
                latitude1 == null
                        || longitude1 == null
                        || latitude2 == null
                        || longitude2 == null
        ) {
            return 0.0;
        }

        double earthRadiusKm =
                6371.0;

        double lat1 =
                Math.toRadians(latitude1);

        double lat2 =
                Math.toRadians(latitude2);

        double deltaLat =
                Math.toRadians(
                        latitude2 - latitude1
                );

        double deltaLon =
                Math.toRadians(
                        longitude2 - longitude1
                );

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        +
                        Math.cos(lat1)
                                * Math.cos(lat2)
                                *
                                Math.sin(deltaLon / 2)
                                        * Math.sin(deltaLon / 2);

        double c =
                2.0 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(
                                1.0 - a
                        )
                );

        return round(
                earthRadiusKm * c
        );
    }

    /*
     * ============================================================
     * UNAVAILABLE ROUTE
     * ============================================================
     */

    private RouteOptionResponse buildUnavailableRoute(
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            TransportMode transportMode,
            String availabilityMessage
    ) {

        List<RouteWeatherPoint> emptyWeather =
                List.of();

        RiskLevel riskLevel =
                routeRiskService.calculateRisk(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        transportMode
                );

        int riskScore =
                routeRiskService.calculateRiskScore(
                        originWeather,
                        destinationWeather,
                        emptyWeather,
                        transportMode
                );

        String weatherRecommendation =
                routeRiskService.getRecommendation(
                        riskLevel
                );

        String recommendation =
                availabilityMessage == null
                        || availabilityMessage.isBlank()
                        ? weatherRecommendation
                        : availabilityMessage
                                + " "
                                + weatherRecommendation;

        return new RouteOptionResponse(
                "UNAVAILABLE",
                null,
                null,
                riskScore,
                riskLevel,
                recommendation,
                true,
                null,
                emptyWeather
        );
    }

    /*
     * ============================================================
     * EXISTING ROUTE SCORING
     * ============================================================
     */

    private void calculateCombinedScores(
            List<RouteOptionResponse> routes
    ) {

        if (routes.isEmpty()) {
            return;
        }

        double minimumDistance =
                routes.stream()
                        .map(
                                RouteOptionResponse::getDistanceKm
                        )
                        .filter(
                                value -> value != null
                        )
                        .min(
                                Comparator.naturalOrder()
                        )
                        .orElse(0.0);

        double maximumDistance =
                routes.stream()
                        .map(
                                RouteOptionResponse::getDistanceKm
                        )
                        .filter(
                                value -> value != null
                        )
                        .max(
                                Comparator.naturalOrder()
                        )
                        .orElse(0.0);

        int minimumDuration =
                routes.stream()
                        .map(
                                RouteOptionResponse::getDurationMinutes
                        )
                        .filter(
                                value -> value != null
                        )
                        .min(
                                Comparator.naturalOrder()
                        )
                        .orElse(0);

        int maximumDuration =
                routes.stream()
                        .map(
                                RouteOptionResponse::getDurationMinutes
                        )
                        .filter(
                                value -> value != null
                        )
                        .max(
                                Comparator.naturalOrder()
                        )
                        .orElse(0);

        for (RouteOptionResponse route :
                routes) {

            /*
             * Only calculate a combined route score
             * when actual route measurements exist.
             */
            if (
                    route.getDistanceKm() == null
                            || route.getDurationMinutes() == null
            ) {

                route.setCombinedScore(
                        0.0
                );

                continue;
            }

            double score =
                    routeRecommendationService
                            .calculateCombinedScore(
                                    route,
                                    minimumDistance,
                                    maximumDistance,
                                    minimumDuration,
                                    maximumDuration
                            );

            route.setCombinedScore(
                    round(score)
            );
        }
    }

    private void markRecommendedRoute(
            List<RouteOptionResponse> routes,
            RouteOptionResponse bestRoute
    ) {

        boolean unavailable =
                bestRoute == null
                        || "UNAVAILABLE".equalsIgnoreCase(
                                bestRoute.getRouteKey()
                        );

        for (RouteOptionResponse route :
                routes) {

            route.setRecommended(
                    !unavailable && route == bestRoute
            );
        }
    }

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}