package com.weatherwise.routing;

import java.util.List;

public record RoutingResponse(
        Double distanceKm,
        Integer durationMinutes,
        String geometry,
        List<RoutingOption> alternatives,
        Boolean mapAvailable,
        String availabilityMessage
) {

    public RoutingResponse(
            Double distanceKm,
            Integer durationMinutes,
            String geometry,
            List<RoutingOption> alternatives
    ) {
        this(
                distanceKm,
                durationMinutes,
                geometry,
                alternatives,
                geometry != null && !geometry.isBlank(),
                geometry != null && !geometry.isBlank()
                        ? "Map route available."
                        : "Map not available for this transport mode."
        );
    }
} 