package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class GtfsScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsScheduleRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String findStopTimezone(
        String feedId,
        String stopId
    ) {

        List<String> timezones =
                jdbcTemplate.query(
                        """
                        SELECT timezone
                        FROM gtfs_stop
                        WHERE feed_id = ?
                        AND stop_id = ?
                        LIMIT 1
                        """,
                        (rs, rowNum) ->
                                rs.getString("timezone"),
                        feedId.trim(),
                        stopId.trim()
                );

        if (timezones.isEmpty()) {
            return null;
        }

        return timezones.get(0);
    }

    public List<GtfsSchedule> findSchedules(
            String feedId,
            String stopId,
            LocalDate date,
            Set<String> activeServices,
            int limit
    ) {

        int safeLimit =
                Math.max(
                        1,
                        Math.min(limit, 500)
                );

        if (feedId == null
                || feedId.isBlank()
                || stopId == null
                || stopId.isBlank()
                || activeServices == null
                || activeServices.isEmpty()) {

            return List.of();
        }

        List<String> services =
                new ArrayList<>(
                        activeServices
                );

        String placeholders =
                String.join(
                        ",",
                        services.stream()
                                .map(item -> "?")
                                .toList()
                );

        /*
         * Do not restrict the SQL query to the first few
         * rows before the departure service filters them.
         *
         * GTFS times can be greater than 24:00:00, so we
         * order by the raw GTFS departure time and retrieve
         * enough records for the departure layer to determine
         * the actual upcoming services.
         */
        String sql = """
                SELECT
                    s.stop_id,
                    s.stop_name,
                    s.timezone AS stop_timezone,

                    st.trip_id,
                    st.arrival_time,
                    st.departure_time,
                    st.stop_sequence,
                    st.stop_headsign,

                    t.route_id,
                    t.service_id,
                    t.trip_headsign,
                    t.direction_id,

                    r.route_short_name,
                    r.route_long_name,
                    r.route_type

                FROM gtfs_stop_time st

                JOIN gtfs_stop s
                    ON s.feed_id = st.feed_id
                    AND s.stop_id = st.stop_id

                JOIN gtfs_trip t
                    ON t.feed_id = st.feed_id
                    AND t.trip_id = st.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                WHERE st.feed_id = ?
                  AND st.stop_id = ?
                  AND t.service_id IN (
                """
                + placeholders
                + """
                  )

                ORDER BY
                    st.departure_time ASC

                LIMIT ?
                """;

        List<Object> parameters =
                new ArrayList<>();

        parameters.add(
                feedId.trim()
        );

        parameters.add(
                stopId.trim()
        );

        parameters.addAll(
                services
        );

        parameters.add(
                safeLimit
        );

        return jdbcTemplate.query(
                sql,
                parameters.toArray(),
                (rs, rowNum) ->
                        new GtfsSchedule(

                                rs.getString(
                                        "stop_id"
                                ),

                                rs.getString(
                                        "stop_name"
                                ),

                                rs.getString(
                                        "stop_timezone"
                                ),

                                rs.getString(
                                        "trip_id"
                                ),

                                rs.getString(
                                        "arrival_time"
                                ),

                                rs.getString(
                                        "departure_time"
                                ),

                                rs.getInt(
                                        "stop_sequence"
                                ),

                                rs.getString(
                                        "stop_headsign"
                                ),

                                rs.getString(
                                        "route_id"
                                ),

                                rs.getString(
                                        "service_id"
                                ),

                                rs.getString(
                                        "trip_headsign"
                                ),

                                rs.getString(
                                        "direction_id"
                                ),

                                rs.getString(
                                        "route_short_name"
                                ),

                                rs.getString(
                                        "route_long_name"
                                ),

                                rs.getString(
                                        "route_type"
                                )
                        )
        );
    }
}