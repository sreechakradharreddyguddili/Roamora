package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsScheduleResponse(

        String feedId,
        String stopId,
        String stopName,

        int count,

        List<GtfsSchedule> schedules
) {
}