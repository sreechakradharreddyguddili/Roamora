package com.weatherwise.transport.gtfs;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record GtfsCalendar(

        String serviceId,

        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday,

        LocalDate startDate,
        LocalDate endDate

) {

    public boolean operatesOnWeekday(
            DayOfWeek day
    ) {

        return switch (day) {

            case MONDAY ->
                    monday;

            case TUESDAY ->
                    tuesday;

            case WEDNESDAY ->
                    wednesday;

            case THURSDAY ->
                    thursday;

            case FRIDAY ->
                    friday;

            case SATURDAY ->
                    saturday;

            case SUNDAY ->
                    sunday;
        };
    }
}