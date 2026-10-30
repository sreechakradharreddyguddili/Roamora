package com.weatherwise.service;

import com.weatherwise.auth.User;
import com.weatherwise.cache.RoutePlanCache;
import com.weatherwise.cache.RoutePlanCacheService;
import com.weatherwise.dto.RouteAnalysisResponse;
import com.weatherwise.dto.RouteOptionResponse;
import com.weatherwise.dto.RouteRequest;
import com.weatherwise.dto.RouteResponse;
import com.weatherwise.entity.Route;
import com.weatherwise.enums.RiskLevel;
import com.weatherwise.exception.RouteNotFoundException;
import com.weatherwise.location.GeocodingService;
import com.weatherwise.location.LocationResponse;
import com.weatherwise.repository.RouteRepository;
import com.weatherwise.security.CurrentUserService;
import com.weatherwise.weather.RouteWeatherPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteService {

    private static final Logger logger =
            LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;

    private final GeocodingService geocodingService;

    private final RoutePlanCacheService routePlanCacheService;

    private final CurrentUserService currentUserService;

    public RouteService(
            RouteRepository routeRepository,
            GeocodingService geocodingService,
            RoutePlanCacheService routePlanCacheService,
            CurrentUserService currentUserService
    ) {
        this.routeRepository = routeRepository;
        this.geocodingService = geocodingService;
        this.routePlanCacheService = routePlanCacheService;
        this.currentUserService = currentUserService;
    }

    public RouteAnalysisResponse planRoute(
            RouteRequest request
    ) {

        logger.info(
                "Planning {} route from {} to {}",
                request.getTransportMode(),
                request.getOrigin(),
                request.getDestination()
        );

        LocationResponse originLocation =
                resolveLocation(
                        request.getOrigin(),
                        request.getOriginLatitude(),
                        request.getOriginLongitude()
                );

        LocationResponse destinationLocation =
                resolveLocation(
                        request.getDestination(),
                        request.getDestinationLatitude(),
                        request.getDestinationLongitude()
                );

        RoutePlanCache cachedPlan =
                routePlanCacheService.calculate(
                        originLocation,
                        destinationLocation,
                        request.getTransportMode(),
                        request.getDepartureDate(),
                        request.getDepartureTime()
                );

        RouteOptionResponse bestRoute =
                cachedPlan.bestRoute();

        if (bestRoute == null) {
            throw new IllegalStateException(
                    "Unable to determine the best route"
            );
        }

        RiskLevel primaryRisk =
                bestRoute.getRiskLevel();

        int primaryRiskScore =
                bestRoute.getRiskScore();

        String primaryRecommendation =
                bestRoute.getRecommendation();

        List<RouteWeatherPoint> primaryWeather =
                bestRoute.getWeatherAlongRoute();

        String selectedGeometry =
                bestRoute.getGeometry();

        /*
         * Route planning does not require authentication.
         *
         * Authenticated users keep the existing route-history
         * behavior.
         *
         * Anonymous users receive the calculated route without
         * persisting it.
         */
        User currentUser =
                getAuthenticatedUserOrNull();

        if (currentUser == null) {

            logger.info(
                    "Route calculated for anonymous request; "
                            + "route will not be persisted."
            );

            RouteAnalysisResponse response =
                    new RouteAnalysisResponse(
                            null,
                            request.getOrigin().trim(),
                            request.getDestination().trim(),
                            request.getTransportMode(),
                            cachedPlan.originWeather().getTemperature(),
                            cachedPlan.destinationWeather().getTemperature(),
                            cachedPlan.originWeather().getWindSpeed(),
                            cachedPlan.destinationWeather().getWindSpeed(),
                            primaryRisk,
                            primaryRiskScore,
                            primaryRecommendation,
                            bestRoute.getDistanceKm(),
                            bestRoute.getDurationMinutes(),
                            selectedGeometry,
                            primaryWeather,
                            cachedPlan.alternatives(),
                            null
                    );

            enrichAnalysisResponse(
                    response,
                    bestRoute,
                    cachedPlan
            );

            return response;
        }

        /*
         * An unavailable provider result is useful to display,
         * but it is not a completed journey and must never be
         * written to user history as a successful route.
         */
        if (isUnavailableRoute(bestRoute)) {
            RouteAnalysisResponse response =
                    new RouteAnalysisResponse(
                            null,
                            request.getOrigin().trim(),
                            request.getDestination().trim(),
                            request.getTransportMode(),
                            cachedPlan.originWeather().getTemperature(),
                            cachedPlan.destinationWeather().getTemperature(),
                            cachedPlan.originWeather().getWindSpeed(),
                            cachedPlan.destinationWeather().getWindSpeed(),
                            primaryRisk,
                            primaryRiskScore,
                            primaryRecommendation,
                            null,
                            null,
                            null,
                            primaryWeather,
                            cachedPlan.alternatives(),
                            null
                    );

            enrichAnalysisResponse(
                    response,
                    bestRoute,
                    cachedPlan
            );

            return response;
        }

        /*
         * Authenticated request:
         * preserve existing route-history behavior.
         */
        Route route =
                new Route();

        route.setUser(currentUser);

        route.setOrigin(
                request.getOrigin().trim()
        );

        route.setDestination(
                request.getDestination().trim()
        );

        route.setTransportMode(
                request.getTransportMode()
        );

        route.setStatus(
                primaryRisk
        );

        route.setRiskScore(
                primaryRiskScore
        );

        route.setRecommendation(
                primaryRecommendation
        );

        route.setDistanceKm(
                bestRoute.getDistanceKm()
        );

        route.setDurationMinutes(
                bestRoute.getDurationMinutes()
        );

        route.setDepartureDate(
                request.getDepartureDate()
        );

        route.setDepartureTime(
                request.getDepartureTime()
        );

        route.setRouteGeometry(
                selectedGeometry
        );

        route.setSaved(false);

        Route savedRoute =
                routeRepository.save(route);

        logger.info(
                "Best route selected: {} with combined score {}",
                bestRoute.getRouteKey(),
                bestRoute.getCombinedScore()
        );

        logger.info(
                "Route saved with id {} for user {}",
                savedRoute.getId(),
                currentUser.getEmail()
        );

        RouteAnalysisResponse response =
                new RouteAnalysisResponse(
                        savedRoute.getId(),
                        savedRoute.getOrigin(),
                        savedRoute.getDestination(),
                        savedRoute.getTransportMode(),
                        cachedPlan.originWeather().getTemperature(),
                        cachedPlan.destinationWeather().getTemperature(),
                        cachedPlan.originWeather().getWindSpeed(),
                        cachedPlan.destinationWeather().getWindSpeed(),
                        primaryRisk,
                        primaryRiskScore,
                        primaryRecommendation,
                        savedRoute.getDistanceKm(),
                        savedRoute.getDurationMinutes(),
                        savedRoute.getRouteGeometry(),
                        primaryWeather,
                        cachedPlan.alternatives(),
                        savedRoute.getCreatedAt()
                );

        enrichAnalysisResponse(
                response,
                bestRoute,
                cachedPlan
        );

        return response;
    }

    private boolean isUnavailableRoute(
            RouteOptionResponse route
    ) {
        return route == null
                || "UNAVAILABLE".equalsIgnoreCase(
                        route.getRouteKey()
                );
    }

    private void enrichAnalysisResponse(
            RouteAnalysisResponse response,
            RouteOptionResponse bestRoute,
            RoutePlanCache cachedPlan
    ) {
        response.setMapAvailable(
                bestRoute != null
                        && bestRoute.getGeometry() != null
                        && !bestRoute.getGeometry().isBlank()
        );

        response.setJourney(
                bestRoute == null
                        ? null
                        : bestRoute.getJourney()
        );

        response.setOriginHumidity(
                cachedPlan.originWeather().getHumidity()
        );

        response.setDestinationHumidity(
                cachedPlan.destinationWeather().getHumidity()
        );

        response.setOriginPrecipitation(
                cachedPlan.originWeather().getPrecipitation()
        );

        response.setDestinationPrecipitation(
                cachedPlan.destinationWeather().getPrecipitation()
        );

        response.setOriginWeatherCode(
                cachedPlan.originWeather().getWeatherCode()
        );

        response.setDestinationWeatherCode(
                cachedPlan.destinationWeather().getWeatherCode()
        );
    }

    /**
     * Uses coordinates selected from the worldwide location
     * search whenever they are available.
     *
     * Falls back to geocoding when coordinates were not supplied.
     */
    private LocationResponse resolveLocation(
            String locationName,
            Double latitude,
            Double longitude
    ) {

        if (
                latitude != null
                        && longitude != null
                        && Double.isFinite(latitude)
                        && Double.isFinite(longitude)
        ) {

            logger.debug(
                    "Using selected coordinates for {}: {}, {}",
                    locationName,
                    latitude,
                    longitude
            );

            return new LocationResponse(
                    locationName.trim(),
                    null,
                    latitude,
                    longitude
            );
        }

        logger.debug(
                "No selected coordinates for {}; falling back to geocoding.",
                locationName
        );

        return geocodingService.findLocation(
                locationName
        );
    }

    private User getAuthenticatedUserOrNull() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication instanceof AnonymousAuthenticationToken
        ) {
            return null;
        }

        try {

            return currentUserService.getCurrentUser();

        } catch (IllegalStateException exception) {

            logger.debug(
                    "No application user available for authenticated context.",
                    exception
            );

            return null;
        }
    }

    public Page<RouteResponse> getAllRoutes(
            Pageable pageable
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        return routeRepository
                .findByUserIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        pageable
                )
                .map(route ->
                        convertToResponse(
                                route,
                                "Route fetched successfully"
                        )
                );
    }

    public Page<RouteResponse> getSavedRoutes(
            Pageable pageable
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        return routeRepository
                .findByUserIdAndSavedTrueOrderByCreatedAtDesc(
                        currentUser.getId(),
                        pageable
                )
                .map(route ->
                        convertToResponse(
                                route,
                                "Saved routes fetched successfully"
                        )
                );
    }

    public RouteResponse getRouteById(
            Long id
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Route route =
                routeRepository
                        .findByIdAndUserId(
                                id,
                                currentUser.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RouteNotFoundException(
                                                "Route not found with id: "
                                                        + id
                                        )
                        );

        return convertToResponse(
                route,
                "Route fetched successfully"
        );
    }

    @Transactional
    public RouteResponse saveRoute(
            Long id
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Route route =
                routeRepository
                        .findByIdAndUserId(
                                id,
                                currentUser.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RouteNotFoundException(
                                                "Route not found with id: "
                                                        + id
                                        )
                        );

        route.setSaved(true);

        Route updatedRoute =
                routeRepository.save(route);

        return convertToResponse(
                updatedRoute,
                "Route saved successfully"
        );
    }

    @Transactional
    public RouteResponse unsaveRoute(
            Long id
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        Route route =
                routeRepository
                        .findByIdAndUserId(
                                id,
                                currentUser.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new RouteNotFoundException(
                                                "Route not found with id: "
                                                        + id
                                        )
                        );

        route.setSaved(false);

        Route updatedRoute =
                routeRepository.save(route);

        return convertToResponse(
                updatedRoute,
                "Route removed from saved routes"
        );
    }

    @Transactional
    public void deleteRoute(
            Long id
    ) {

        User currentUser =
                currentUserService.getCurrentUser();

        if (
                !routeRepository.existsByIdAndUserId(
                        id,
                        currentUser.getId()
                )
        ) {

            throw new RouteNotFoundException(
                    "Route not found with id: " + id
            );
        }

        routeRepository.deleteByIdAndUserId(
                id,
                currentUser.getId()
        );
    }

    @Transactional
    public void clearHistory() {

        User currentUser =
                currentUserService.getCurrentUser();

        routeRepository.deleteByUserIdAndSavedFalse(
                currentUser.getId()
        );
    }

    private RouteResponse convertToResponse(
            Route route,
            String message
    ) {

        RouteResponse response =
                new RouteResponse(
                        route.getId(),
                        message,
                        route.getOrigin(),
                        route.getDestination(),
                        route.getTransportMode(),
                        route.getStatus(),
                        route.getDistanceKm(),
                        route.getDurationMinutes(),
                        route.getRiskScore(),
                        route.getRecommendation(),
                        route.isSaved(),
                        route.getCreatedAt()
                );

        response.setDepartureDate(
                route.getDepartureDate()
        );

        response.setDepartureTime(
                route.getDepartureTime()
        );

        return response;
    }
}