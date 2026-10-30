package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class GtfsScheduleService {

    private final GtfsScheduleRepository repository;

    private final GtfsServiceCalendarService
            calendarService;

    public GtfsScheduleService(
            GtfsScheduleRepository repository,
            GtfsServiceCalendarService calendarService
    ) {

        this.repository =
                repository;

        this.calendarService =
                calendarService;
    }

    public GtfsScheduleResponse findSchedules(
            String feedId,
            String stopId,
            LocalDate date,
            int limit
    ) {

        if (feedId == null
                || feedId.isBlank()) {

            throw new IllegalArgumentException(
                    "feedId is required."
            );
        }

        if (stopId == null
                || stopId.isBlank()) {

            throw new IllegalArgumentException(
                    "stopId is required."
            );
        }

        LocalDate requestedDate =
                date == null
                        ? LocalDate.now()
                        : date;

        Set<String> activeServices =
                calendarService.findActiveServices(
                        feedId.trim(),
                        requestedDate
                );

        List<GtfsSchedule> schedules =
                repository.findSchedules(
                        feedId.trim(),
                        stopId.trim(),
                        requestedDate,
                        activeServices,
                        limit
                );

        String stopName =
                schedules.isEmpty()
                        ? null
                        : schedules
                                .get(0)
                                .stopName();

        return new GtfsScheduleResponse(
                feedId.trim(),
                stopId.trim(),
                stopName,
                schedules.size(),
                schedules
        );
    }
}