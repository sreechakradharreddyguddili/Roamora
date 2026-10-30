package com.weatherwise.transport.gtfs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GtfsDepartureResponse(

        String feedId,

        String stopId,

        String stopName,

        LocalDate date,

        LocalTime currentTime,

        int count,

        List<GtfsDeparture> departures
) {

    public GtfsDepartureResponse {

        departures = departures == null
                ? List.of()
                : List.copyOf(departures);

        count = departures.size();
    }
}