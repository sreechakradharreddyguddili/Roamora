package com.weatherwise.transport.flight;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.location.GeocodingService;
import com.weatherwise.location.LocationResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/multimodal/flight-transit")
public class FlightTransitConnectionController {

    private final GeocodingService geocodingService;

    private final FlightTransitConnectionService
            connectionService;

    public FlightTransitConnectionController(
            GeocodingService geocodingService,
            FlightTransitConnectionService connectionService
    ) {

        this.geocodingService =
                geocodingService;

        this.connectionService =
                connectionService;
    }

    @GetMapping
    public FlightTransitConnectionResponse plan(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam LocalDate departureDate,
            @RequestParam(
                    defaultValue = "TRAIN"
            )
            TransportMode onwardTransportMode
    ) {

        LocationResponse originLocation =
                geocodingService.findLocation(
                        origin
                );

        LocationResponse destinationLocation =
                geocodingService.findLocation(
                        destination
                );

        return connectionService.plan(
                originLocation,
                destinationLocation,
                departureDate,
                onwardTransportMode
        );
    }
}