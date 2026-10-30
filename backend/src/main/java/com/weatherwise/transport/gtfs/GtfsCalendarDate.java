package com.weatherwise.transport.gtfs;

import java.time.LocalDate;

public record GtfsCalendarDate(

        String serviceId,

        LocalDate exceptionDate,

        Integer exceptionType

) {

    public boolean isAdded() {

        return exceptionType != null
                && exceptionType == 1;
    }

    public boolean isRemoved() {

        return exceptionType != null
                && exceptionType == 2;
    }
}