package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsFeedData(

        List<GtfsStop> stops,

        List<GtfsRoute> routes,

        List<GtfsTrip> trips,

        List<GtfsStopTime> stopTimes,

        List<GtfsCalendar> calendars,

        List<GtfsCalendarDate> calendarDates

) {

    public GtfsFeedData {

        stops =
                stops == null
                        ? List.of()
                        : List.copyOf(stops);

        routes =
                routes == null
                        ? List.of()
                        : List.copyOf(routes);

        trips =
                trips == null
                        ? List.of()
                        : List.copyOf(trips);

        stopTimes =
                stopTimes == null
                        ? List.of()
                        : List.copyOf(stopTimes);

        calendars =
                calendars == null
                        ? List.of()
                        : List.copyOf(calendars);

        calendarDates =
                calendarDates == null
                        ? List.of()
                        : List.copyOf(calendarDates);
    }
}