package com.weatherwise.transport.model;

import java.time.OffsetDateTime;
import java.util.List;

public record TransportJourney(
        String journeyId,

        String originName,
        String destinationName,

        Double originLatitude,
        Double originLongitude,

        Double destinationLatitude,
        Double destinationLongitude,

        OffsetDateTime departureTime,
        OffsetDateTime arrivalTime,

        Long totalDurationSeconds,
        Long totalWaitingTimeSeconds,
        Long totalTransferTimeSeconds,

        Integer numberOfTransfers,

        String status,

        List<TransportLeg> legs
) {}