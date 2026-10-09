package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class GtfsServiceCalendarService {
    
    private final JdbcTemplate jdbcTemplate;

    public int countCalendars(String feedId) {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM gtfs_calendar
                        WHERE feed_id = ?
                        """,
                        Integer.class,
                        feedId
                );

        return count == null ? 0 : count;
    }

    public int countCalendarDates(String feedId) {

        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM gtfs_calendar_date
                        WHERE feed_id = ?
                        """,
                        Integer.class,
                        feedId
                );

        return count == null ? 0 : count;
    }

    public GtfsServiceCalendarService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> findActiveServices(
            String feedId,
            LocalDate date
    ) {

        if (feedId == null
                || feedId.isBlank()) {
            return Set.of();
        }

        if (date == null) {
            return Set.of();
        }

        List<GtfsCalendar> calendars =
                jdbcTemplate.query(
                        """
                        SELECT
                            service_id,
                            monday,
                            tuesday,
                            wednesday,
                            thursday,
                            friday,
                            saturday,
                            sunday,
                            start_date,
                            end_date
                        FROM gtfs_calendar
                        WHERE feed_id = ?
                          AND start_date <= ?
                          AND end_date >= ?
                        """,
                        (rs, rowNum) ->
                                new GtfsCalendar(
                                        rs.getString(
                                                "service_id"
                                        ),

                                        rs.getBoolean(
                                                "monday"
                                        ),

                                        rs.getBoolean(
                                                "tuesday"
                                        ),

                                        rs.getBoolean(
                                                "wednesday"
                                        ),

                                        rs.getBoolean(
                                                "thursday"
                                        ),

                                        rs.getBoolean(
                                                "friday"
                                        ),

                                        rs.getBoolean(
                                                "saturday"
                                        ),

                                        rs.getBoolean(
                                                "sunday"
                                        ),

                                        rs.getObject(
                                                "start_date",
                                                LocalDate.class
                                        ),

                                        rs.getObject(
                                                "end_date",
                                                LocalDate.class
                                        )
                                ),
                        feedId,
                        date,
                        date
                );

        Set<String> active =
                new HashSet<>();

        DayOfWeek day =
                date.getDayOfWeek();

        for (GtfsCalendar calendar : calendars) {

            if (calendar.operatesOnWeekday(day)) {

                active.add(
                        calendar.serviceId()
                );
            }
        }

        /*
         * Apply calendar_dates.txt exceptions.
         */
        List<GtfsCalendarDate> exceptions =
                jdbcTemplate.query(
                        """
                        SELECT
                            service_id,
                            exception_date,
                            exception_type
                        FROM gtfs_calendar_date
                        WHERE feed_id = ?
                          AND exception_date = ?
                        """,
                        (rs, rowNum) ->
                                new GtfsCalendarDate(
                                        rs.getString(
                                                "service_id"
                                        ),

                                        rs.getObject(
                                                "exception_date",
                                                LocalDate.class
                                        ),

                                        rs.getInt(
                                                "exception_type"
                                        )
                                ),
                        feedId,
                        date
                );

        for (GtfsCalendarDate exception :
                exceptions) {

            if (exception.isAdded()) {

                active.add(
                        exception.serviceId()
                );

            } else if (exception.isRemoved()) {

                active.remove(
                        exception.serviceId()
                );
            }
        }

        return active;
    }
}