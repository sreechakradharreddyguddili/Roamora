package com.weatherwise.routing;

public record RoutingOption(
        String routeKey,
        Double distanceKm,
        Integer durationMinutes,
        String geometry
) {
}