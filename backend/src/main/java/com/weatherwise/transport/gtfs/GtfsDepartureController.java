package com.weatherwise.transport.gtfs;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/gtfs")
public class GtfsDepartureController {

    private final GtfsDepartureService
            departureService;

    public GtfsDepartureController(
            GtfsDepartureService departureService
    ) {

        this.departureService =
                departureService;
    }

    @GetMapping("/departures")
    public GtfsDepartureResponse findDepartures(

            @RequestParam
            String feedId,

            @RequestParam
            String stopId,

            @RequestParam(required = false)
            LocalDate date,

            @RequestParam(defaultValue = "10")
            Integer limit
    ) {

        return departureService.findDepartures(
                feedId,
                stopId,
                date,
                limit
        );
    }
}