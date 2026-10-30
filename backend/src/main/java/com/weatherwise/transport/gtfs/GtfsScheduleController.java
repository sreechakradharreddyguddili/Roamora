package com.weatherwise.transport.gtfs;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/gtfs")
public class GtfsScheduleController {

    private final GtfsScheduleService
            gtfsScheduleService;

    private final GtfsServiceCalendarService
            calendarService;

    public GtfsScheduleController(
            GtfsScheduleService gtfsScheduleService,
            GtfsServiceCalendarService calendarService
    ) {

        this.gtfsScheduleService =
                gtfsScheduleService;

        this.calendarService =
                calendarService;
    }

    @GetMapping("/schedules")
    public GtfsScheduleResponse findSchedules(

            @RequestParam
            String feedId,

            @RequestParam
            String stopId,

            @RequestParam(required = false)
            LocalDate date,

            @RequestParam(defaultValue = "50")
            int limit
    ) {

        return gtfsScheduleService.findSchedules(
                feedId,
                stopId,
                date,
                limit
        );
    }

    @GetMapping("/calendar-debug")
    public Map<String, Object> calendarDebug(

            @RequestParam
            String feedId,

            @RequestParam
            LocalDate date
    ) {

        Set<String> activeServices =
                calendarService.findActiveServices(
                        feedId.trim(),
                        date
                );

        return Map.of(
                "feedId",
                feedId,

                "date",
                date,

                "activeServiceCount",
                activeServices.size(),

                "activeServices",
                activeServices
        );
    }
    @GetMapping("/calendar-count")
    public Map<String, Object> calendarCount(
            @RequestParam String feedId
    ) {
        return Map.of(
                "feedId", feedId,
                "calendarCount",
                calendarService.countCalendars(feedId.trim()),

                "calendarDateCount",
                calendarService.countCalendarDates(feedId.trim())
        );
    }
}