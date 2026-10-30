package com.weatherwise.transport.gtfs;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gtfs/journeys")
public class GtfsJourneyController {

    private final GtfsJourneyService journeyService;

    public GtfsJourneyController(
            GtfsJourneyService journeyService
    ) {
        this.journeyService = journeyService;
    }

    @GetMapping
    public GtfsJourneyResponse findJourneys(
            @RequestParam String feedId,

            @RequestParam String originStopId,

            @RequestParam String destinationStopId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(defaultValue = "20")
            int limit
    ) {

        int safeLimit =
                Math.max(
                        1,
                        Math.min(
                                limit,
                                100
                        )
                );

        List<GtfsJourney> journeys;

        if (date != null) {

            journeys =
                    journeyService.findJourneysForDate(
                            feedId,
                            originStopId,
                            destinationStopId,
                            date,
                            safeLimit
                    );

        } else {

            journeys =
                    journeyService.findJourneys(
                            feedId,
                            originStopId,
                            destinationStopId,
                            safeLimit
                    );
        }

        String originStopName =
                journeys.isEmpty()
                        ? null
                        : journeys.get(0).originStopName();

        String destinationStopName =
                journeys.isEmpty()
                        ? null
                        : journeys.get(0).destinationStopName();

        return new GtfsJourneyResponse(
                feedId,
                originStopId,
                originStopName,
                destinationStopId,
                destinationStopName,
                date,
                journeys.size(),
                journeys
        );
    }
}