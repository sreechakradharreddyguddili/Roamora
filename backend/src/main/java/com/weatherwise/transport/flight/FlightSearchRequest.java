package com.weatherwise.transport.flight;

import java.time.LocalDate;

public record FlightSearchRequest(
        String originAirportCode,
        String destinationAirportCode,
        LocalDate departureDate,
        String originAirportId,
        String destinationAirportId
) {
    public FlightSearchRequest {
        originAirportCode =
                normalize(originAirportCode);

        destinationAirportCode =
                normalize(destinationAirportCode);

        originAirportId =
                normalize(originAirportId);

        destinationAirportId =
                normalize(destinationAirportId);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
