package com.weatherwise.transport.web;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.transport.model.TransportSearchResponse;
import com.weatherwise.transport.service.TransportJourneyService;

import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transport")
public class TransportJourneyController {

    private final TransportJourneyService transportJourneyService;

    public TransportJourneyController(
            TransportJourneyService transportJourneyService
    ) {
        this.transportJourneyService = transportJourneyService;
    }

    @GetMapping("/journeys")
    public TransportSearchResponse journeys(

            @RequestParam String origin,

            @RequestParam String destination,

            @RequestParam(required = false)
            String departureTime,

            @RequestParam(required = false)
            String modes
    ) {

        OffsetDateTime requestedTime =
                departureTime == null || departureTime.isBlank()
                        ? OffsetDateTime.now()
                        : OffsetDateTime.parse(departureTime);

        List<TransportMode> requestedModes =
                modes == null || modes.isBlank()
                        ? List.of(TransportMode.MULTIMODAL)
                        : Arrays.stream(modes.split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(TransportMode::valueOf)
                        .toList();

        return transportJourneyService.search(
                origin,
                destination,
                requestedTime,
                requestedModes
        );
    }
}