package com.weatherwise.transport.flight;

import java.util.List;

public record FlightSearchResponse(
        String status,
        String message,
        String provider,
        List<FlightLeg> flights
) {
    public FlightSearchResponse {
        flights =
                flights == null
                        ? List.of()
                        : List.copyOf(flights);
    }

    public static FlightSearchResponse unavailable(
            String message
    ) {
        return new FlightSearchResponse(
                "UNAVAILABLE",
                message,
                null,
                List.of()
        );
    }

    public static FlightSearchResponse available(
            String provider,
            List<FlightLeg> flights
    ) {
        return new FlightSearchResponse(
                flights == null || flights.isEmpty()
                        ? "NO_RESULTS"
                        : "AVAILABLE",
                flights == null || flights.isEmpty()
                        ? "No scheduled flights were found."
                        : "Scheduled flights found.",
                provider,
                flights
        );
    }
}
