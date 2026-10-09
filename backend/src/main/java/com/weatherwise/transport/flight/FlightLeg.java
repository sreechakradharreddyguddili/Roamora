package com.weatherwise.transport.flight;

import java.time.OffsetDateTime;
import java.time.Duration;

public record FlightLeg(
        String provider,
        String flightId,
        String airlineCode,
        String airlineName,
        String flightNumber,
        String originAirportId,
        String originAirportCode,
        String originAirportName,
        String destinationAirportId,
        String destinationAirportCode,
        String destinationAirportName,
        OffsetDateTime scheduledDeparture,
        OffsetDateTime scheduledArrival,
        OffsetDateTime estimatedDeparture,
        OffsetDateTime estimatedArrival,
        Integer delayMinutes,
        Duration flightDuration,
        FlightStatus status,
        String aircraftType,
        String terminal,
        String gate,
        Boolean liveDataAvailable
) {
}
