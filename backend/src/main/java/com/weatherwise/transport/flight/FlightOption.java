package com.weatherwise.transport.flight;

import java.time.OffsetDateTime;

public record FlightOption(
        String flightNumber,

        String airline,

        String originAirport,
        String destinationAirport,

        OffsetDateTime scheduledDeparture,
        OffsetDateTime scheduledArrival,

        OffsetDateTime estimatedDeparture,
        OffsetDateTime estimatedArrival,

        String status,

        String departureTerminal,
        String departureGate,

        String arrivalTerminal,
        String arrivalGate
) {}