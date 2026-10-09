package com.weatherwise.transport.flight;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/flights")
public class FlightNetworkController {

    private final FlightNetworkService flightNetworkService;

    public FlightNetworkController(
            FlightNetworkService flightNetworkService
    ) {
        this.flightNetworkService =
                flightNetworkService;
    }

    @GetMapping("/search")
    public FlightSearchResponse search(
            @RequestParam(required = false)
            String originAirportCode,

            @RequestParam(required = false)
            String destinationAirportCode,

            @RequestParam(required = false)
            String originAirportId,

            @RequestParam(required = false)
            String destinationAirportId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate departureDate
    ) {
        return flightNetworkService.search(
                new FlightSearchRequest(
                        originAirportCode,
                        destinationAirportCode,
                        departureDate,
                        originAirportId,
                        destinationAirportId
                )
        );
    }
}
