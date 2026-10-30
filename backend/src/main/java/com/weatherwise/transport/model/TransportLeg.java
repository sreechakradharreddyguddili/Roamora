package com.weatherwise.transport.model;

import com.weatherwise.enums.TransportMode;

import java.time.OffsetDateTime;

public record TransportLeg(
        TransportMode mode,

        String fromId,
        String fromName,
        Double fromLatitude,
        Double fromLongitude,

        String toId,
        String toName,
        Double toLatitude,
        Double toLongitude,

        OffsetDateTime scheduledDepartureTime,
        OffsetDateTime scheduledArrivalTime,

        OffsetDateTime estimatedDepartureTime,
        OffsetDateTime estimatedArrivalTime,

        OffsetDateTime actualDepartureTime,
        OffsetDateTime actualArrivalTime,

        Long durationSeconds,
        Long waitingTimeSeconds,
        Long transferTimeSeconds,

        String status,

        String operatorName,
        String routeId,
        String routeNumber,
        String routeName,

        String platform,
        String gate,

        String vehicleId,
        String vehicleName,

        String geometry,

        boolean mapAvailable,
        boolean liveDataAvailable,
        boolean bookingRequired
) {}