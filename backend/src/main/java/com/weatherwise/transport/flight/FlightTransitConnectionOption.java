package com.weatherwise.transport.flight;

import com.weatherwise.location.TransportPlace;
import com.weatherwise.transport.gtfs.GtfsJourney;

import java.time.OffsetDateTime;

public record FlightTransitConnectionOption(
        String optionId,
        TransportPlace originAirport,
        TransportPlace destinationAirport,
        FlightLeg flight,
        TransportPlace onwardPlace,
        TransportPlace journeyDestinationPlace,
        GtfsJourney journey,
        Integer originAccessMinutes,
        Integer airportDepartureBufferMinutes,
        Integer airportToTransitMinutes,
        Integer flightDurationMinutes,
        Integer flightToTransitWaitingMinutes,
        Integer transitDurationMinutes,
        Integer finalJourneyMinutes,
        Integer totalJourneyMinutes,
        OffsetDateTime flightDeparture,
        OffsetDateTime flightArrival,
        String status,
        String message
) {
    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }
}
