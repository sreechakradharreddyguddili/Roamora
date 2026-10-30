package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;

public record MultimodalConnector(
        TransportMode mode,
        String status,
        Double distanceKm,
        Integer durationMinutes,
        String from,
        String to,
        String message
) {
}
