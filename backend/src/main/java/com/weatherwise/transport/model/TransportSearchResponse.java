package com.weatherwise.transport.model;

import java.util.List;

public record TransportSearchResponse(
        String origin,
        String destination,

        boolean available,

        String message,

        List<TransportJourney> journeys
) {}