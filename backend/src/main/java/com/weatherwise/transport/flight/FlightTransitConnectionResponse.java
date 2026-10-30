package com.weatherwise.transport.flight;

import java.util.List;

public record FlightTransitConnectionResponse(

        String status,

        String message,

        String origin,

        String destination,

        String onwardTransportMode,

        List<FlightTransitConnectionOption> options

) {

    public static FlightTransitConnectionResponse unavailable(
            String origin,
            String destination,
            String onwardTransportMode,
            String message
    ) {

        return new FlightTransitConnectionResponse(
                "UNAVAILABLE",
                message,
                origin,
                destination,
                onwardTransportMode,
                List.of()
        );
    }

    public static FlightTransitConnectionResponse available(
            String origin,
            String destination,
            String onwardTransportMode,
            List<FlightTransitConnectionOption> options
    ) {

        return new FlightTransitConnectionResponse(
                "AVAILABLE",
                "Flight and onward transit connections found.",
                origin,
                destination,
                onwardTransportMode,
                options
        );
    }
}