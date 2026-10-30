package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Repository
public class GtfsTransitGraphRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsTransitGraphRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds all usable transit connections leaving a stop
     * for services active on the requested service date.
     *
     * A connection represents:
     *
     *     Stop A -> Stop B
     *
     * on one specific trip.
     *
     * This is the building block for the multi-leg journey
     * planner.
     */
    public List<GtfsTransitConnection> findConnectionsFromStop(
            String feedId,
            String originStopId,
            List<String> activeServiceIds,
            int limit
    ) {

        if (!isValidFeed(feedId) ||
                !isValidStop(originStopId)) {

            return List.of();
        }

        List<String> services =
                normalizeServices(
                        activeServiceIds
                );

        if (services.isEmpty()) {
            return List.of();
        }

        int safeLimit =
                normalizeLimit(limit);

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                services.size(),
                                "?"
                        )
                );

        /*
         * Each connection is between two consecutive stops
         * on the same trip.
         *
         * Example:
         *
         * Trip T1:
         *
         * A -> B -> C -> D
         *
         * Connections:
         *
         * A -> B
         * B -> C
         * C -> D
         *
         * This prevents the graph from becoming unnecessarily
         * large while still allowing the journey planner to
         * chain multiple connections.
         */
        String sql = """
                SELECT
                    s1.feed_id,

                    s1.stop_id AS origin_stop_id,
                    s1.stop_name AS origin_stop_name,
                    origin_stop.parent_station AS origin_parent_station,

                    s2.stop_id AS destination_stop_id,
                    s2.stop_name AS destination_stop_name,
                    destination_stop.parent_station AS destination_parent_station,

                    t.trip_id,

                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    s1.departure_time,
                    s2.arrival_time,

                    t.trip_headsign,

                    s1.stop_sequence AS from_stop_sequence,
                    s2.stop_sequence AS to_stop_sequence

                FROM gtfs_stop_time s1

                JOIN gtfs_stop_time s2
                    ON s2.feed_id = s1.feed_id
                    AND s2.trip_id = s1.trip_id
                    AND s2.stop_sequence =
                        s1.stop_sequence + 1

                JOIN gtfs_trip t
                    ON t.feed_id = s1.feed_id
                    AND t.trip_id = s1.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop origin_stop
                    ON origin_stop.feed_id = s1.feed_id
                    AND origin_stop.stop_id = s1.stop_id

                JOIN gtfs_stop destination_stop
                    ON destination_stop.feed_id = s2.feed_id
                    AND destination_stop.stop_id = s2.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = s1.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE

                  AND s1.stop_id = ?

                  AND t.service_id IN (%s)

                  AND s1.departure_time IS NOT NULL
                  AND s2.arrival_time IS NOT NULL

                ORDER BY
                    s1.departure_time ASC,
                    s1.trip_id ASC,
                    s1.stop_sequence ASC

                LIMIT ?
                """.formatted(
                        placeholders
                );

        Object[] parameters =
                new Object[
                        2 +
                        services.size() +
                        1
                ];

        int index = 0;

        parameters[index++] = feedId;
        parameters[index++] = originStopId;

        for (String serviceId : services) {
            parameters[index++] = serviceId;
        }

        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                this::mapConnection
        );
    }

    /**
     * Finds connections leaving any platform/stop belonging to
     * the supplied parent station.
     */
    public List<GtfsTransitConnection> findConnectionsFromParentStation(
            String feedId,
            String parentStationId,
            List<String> activeServiceIds,
            int limit
    ) {

        if (!isValidFeed(feedId) ||
                !isValidStop(parentStationId)) {
            return List.of();
        }

        List<String> services =
                normalizeServices(activeServiceIds);

        if (services.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                services.size(),
                                "?"
                        )
                );

        String sql = """
                SELECT
                    s1.feed_id,

                    s1.stop_id AS origin_stop_id,
                    s1.stop_name AS origin_stop_name,
                    origin_stop.parent_station AS origin_parent_station,

                    s2.stop_id AS destination_stop_id,
                    s2.stop_name AS destination_stop_name,
                    destination_stop.parent_station AS destination_parent_station,

                    t.trip_id,

                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    s1.departure_time,
                    s2.arrival_time,

                    t.trip_headsign,

                    s1.stop_sequence AS from_stop_sequence,
                    s2.stop_sequence AS to_stop_sequence

                FROM gtfs_stop_time s1

                JOIN gtfs_stop_time s2
                    ON s2.feed_id = s1.feed_id
                    AND s2.trip_id = s1.trip_id
                    AND s2.stop_sequence =
                        s1.stop_sequence + 1

                JOIN gtfs_trip t
                    ON t.feed_id = s1.feed_id
                    AND t.trip_id = s1.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop origin_stop
                    ON origin_stop.feed_id = s1.feed_id
                    AND origin_stop.stop_id = s1.stop_id

                JOIN gtfs_stop destination_stop
                    ON destination_stop.feed_id = s2.feed_id
                    AND destination_stop.stop_id = s2.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = s1.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE
                  AND origin_stop.parent_station = ?
                  AND t.service_id IN (%s)
                  AND s1.departure_time IS NOT NULL
                  AND s2.arrival_time IS NOT NULL

                ORDER BY
                    s1.departure_time ASC,
                    s1.trip_id ASC,
                    s1.stop_sequence ASC

                LIMIT ?
                """.formatted(placeholders);

        Object[] parameters =
                new Object[
                        2 + services.size() + 1
                ];

        int index = 0;

        parameters[index++] = feedId;
        parameters[index++] = parentStationId;

        for (String serviceId : services) {
            parameters[index++] = serviceId;
        }

        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                this::mapConnection
        );
    }

    /**
     * Finds connections from multiple stops.
     *
     * This is useful when the journey planner has several
     * candidate stops around the current search position.
     */
    public List<GtfsTransitConnection> findConnectionsFromStops(
            String feedId,
            List<String> originStopIds,
            List<String> activeServiceIds,
            int limit
    ) {

        if (!isValidFeed(feedId)) {
            return List.of();
        }

        if (originStopIds == null ||
                originStopIds.isEmpty()) {

            return List.of();
        }

        List<String> stops =
                originStopIds.stream()
                        .filter(this::isValidStop)
                        .distinct()
                        .toList();

        if (stops.isEmpty()) {
            return List.of();
        }

        List<String> services =
                normalizeServices(
                        activeServiceIds
                );

        if (services.isEmpty()) {
            return List.of();
        }

        int safeLimit =
                normalizeLimit(limit);

        String stopPlaceholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                stops.size(),
                                "?"
                        )
                );

        String servicePlaceholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                services.size(),
                                "?"
                        )
                );

        String sql = """
                SELECT
                    s1.feed_id,

                    s1.stop_id AS origin_stop_id,
                    s1.stop_name AS origin_stop_name,
                    origin_stop.parent_station AS origin_parent_station,

                    s2.stop_id AS destination_stop_id,
                    s2.stop_name AS destination_stop_name,
                    destination_stop.parent_station AS destination_parent_station,

                    t.trip_id,

                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    s1.departure_time,
                    s2.arrival_time,

                    t.trip_headsign,

                    s1.stop_sequence AS from_stop_sequence,
                    s2.stop_sequence AS to_stop_sequence

                FROM gtfs_stop_time s1

                JOIN gtfs_stop_time s2
                    ON s2.feed_id = s1.feed_id
                    AND s2.trip_id = s1.trip_id
                    AND s2.stop_sequence =
                        s1.stop_sequence + 1

                JOIN gtfs_trip t
                    ON t.feed_id = s1.feed_id
                    AND t.trip_id = s1.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop origin_stop
                    ON origin_stop.feed_id = s1.feed_id
                    AND origin_stop.stop_id = s1.stop_id

                JOIN gtfs_stop destination_stop
                    ON destination_stop.feed_id = s2.feed_id
                    AND destination_stop.stop_id = s2.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = s1.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE

                  AND s1.stop_id IN (%s)

                  AND t.service_id IN (%s)

                  AND s1.departure_time IS NOT NULL
                  AND s2.arrival_time IS NOT NULL

                ORDER BY
                    s1.departure_time ASC,
                    s1.trip_id ASC,
                    s1.stop_sequence ASC

                LIMIT ?
                """.formatted(
                        stopPlaceholders,
                        servicePlaceholders
                );

        Object[] parameters =
                new Object[
                        1 +
                        stops.size() +
                        services.size() +
                        1
                ];

        int index = 0;

        parameters[index++] = feedId;

        for (String stopId : stops) {
            parameters[index++] = stopId;
        }

        for (String serviceId : services) {
            parameters[index++] = serviceId;
        }

        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                this::mapConnection
        );
    }

    /**
     * Finds connections arriving at a destination stop.
     *
     * This allows the multi-leg planner to work backwards or
     * validate whether a candidate connection can reach the
     * requested destination.
     */
    public List<GtfsTransitConnection> findConnectionsToStop(
            String feedId,
            String destinationStopId,
            List<String> activeServiceIds,
            int limit
    ) {

        if (!isValidFeed(feedId) ||
                !isValidStop(destinationStopId)) {

            return List.of();
        }

        List<String> services =
                normalizeServices(
                        activeServiceIds
                );

        if (services.isEmpty()) {
            return List.of();
        }

        int safeLimit =
                normalizeLimit(limit);

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                services.size(),
                                "?"
                        )
                );

        String sql = """
                SELECT
                    s1.feed_id,

                    s1.stop_id AS origin_stop_id,
                    s1.stop_name AS origin_stop_name,
                    origin_stop.parent_station AS origin_parent_station,

                    s2.stop_id AS destination_stop_id,
                    s2.stop_name AS destination_stop_name,
                    destination_stop.parent_station AS destination_parent_station,

                    t.trip_id,

                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    s1.departure_time,
                    s2.arrival_time,

                    t.trip_headsign,

                    s1.stop_sequence AS from_stop_sequence,
                    s2.stop_sequence AS to_stop_sequence

                FROM gtfs_stop_time s1

                JOIN gtfs_stop_time s2
                    ON s2.feed_id = s1.feed_id
                    AND s2.trip_id = s1.trip_id
                    AND s2.stop_sequence =
                        s1.stop_sequence + 1

                JOIN gtfs_trip t
                    ON t.feed_id = s1.feed_id
                    AND t.trip_id = s1.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop origin_stop
                    ON origin_stop.feed_id = s1.feed_id
                    AND origin_stop.stop_id = s1.stop_id

                JOIN gtfs_stop destination_stop
                    ON destination_stop.feed_id = s2.feed_id
                    AND destination_stop.stop_id = s2.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = s1.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE

                  AND s2.stop_id = ?

                  AND t.service_id IN (%s)

                  AND s1.departure_time IS NOT NULL
                  AND s2.arrival_time IS NOT NULL

                ORDER BY
                    s2.arrival_time ASC,
                    s1.departure_time ASC

                LIMIT ?
                """.formatted(
                        placeholders
                );

        Object[] parameters =
                new Object[
                        2 +
                        services.size() +
                        1
                ];

        int index = 0;

        parameters[index++] = feedId;
        parameters[index++] = destinationStopId;

        for (String serviceId : services) {
            parameters[index++] = serviceId;
        }

        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                this::mapConnection
        );
    }

    /**
     * Converts one database row into a transit connection.
     */
    private GtfsTransitConnection mapConnection(
            ResultSet rs,
            int rowNum
    ) throws SQLException {

        String departure =
                rs.getString(
                        "departure_time"
                );

        String arrival =
                rs.getString(
                        "arrival_time"
                );

        LocalDateTime departureTime =
                parseGtfsDateTime(
                        departure
                );

        LocalDateTime arrivalTime =
                parseGtfsDateTime(
                        arrival
                );

        Duration duration =
                Duration.ZERO;

        if (departureTime != null &&
                arrivalTime != null) {

            long minutes =
                    Duration.between(
                            departureTime,
                            arrivalTime
                    ).toMinutes();

            duration =
                    Duration.ofMinutes(
                            Math.max(
                                    0,
                                    minutes
                            )
                    );
        }

        return new GtfsTransitConnection(

                rs.getString(
                        "feed_id"
                ),

                rs.getString(
                        "origin_stop_id"
                ),

                rs.getString(
                        "origin_stop_name"
                ),

                rs.getString(
                        "origin_parent_station"
                ),

                rs.getString(
                        "destination_stop_id"
                ),

                rs.getString(
                        "destination_stop_name"
                ),

                rs.getString(
                        "destination_parent_station"
                ),

                rs.getString(
                        "trip_id"
                ),

                rs.getString(
                        "route_id"
                ),

                rs.getString(
                        "route_short_name"
                ),

                rs.getString(
                        "route_long_name"
                ),

                rs.getString(
                        "route_type"
                ),

                rs.getString(
                        "service_id"
                ),

                departureTime,
                arrivalTime,

                duration,

                rs.getString(
                        "trip_headsign"
                ),

                getInteger(
                        rs,
                        "from_stop_sequence"
                ),

                getInteger(
                        rs,
                        "to_stop_sequence"
                )
        );
    }

    /**
     * Supports standard GTFS times and times beyond midnight.
     *
     * Examples:
     *
     * 08:30:00
     * 23:59:00
     * 25:15:00
     * 26:40:00
     */
    private LocalDateTime parseGtfsDateTime(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        String normalized =
                value.trim();

        String[] parts =
                normalized.split(":");

        if (parts.length != 3) {
            return null;
        }

        try {

            int hour =
                    Integer.parseInt(
                            parts[0]
                    );

            int minute =
                    Integer.parseInt(
                            parts[1]
                    );

            int second =
                    Integer.parseInt(
                            parts[2]
                    );

            if (hour < 0 ||
                    minute < 0 ||
                    minute > 59 ||
                    second < 0 ||
                    second > 59) {

                return null;
            }

            int extraDays =
                    hour / 24;

            int normalizedHour =
                    hour % 24;

            return LocalDateTime.of(
                    1970,
                    1,
                    1,
                    normalizedHour,
                    minute,
                    second
            ).plusDays(
                    extraDays
            );

        } catch (NumberFormatException ex) {

            return null;
        }
    }

    private Integer getInteger(
            ResultSet rs,
            String column
    ) throws SQLException {

        int value =
                rs.getInt(column);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }

    private List<String> normalizeServices(
            List<String> activeServiceIds
    ) {

        if (activeServiceIds == null ||
                activeServiceIds.isEmpty()) {

            return List.of();
        }

        return activeServiceIds.stream()
                .filter(id ->
                        id != null &&
                        !id.isBlank()
                )
                .distinct()
                .toList();
    }

    private boolean isValidFeed(
            String feedId
    ) {

        return feedId != null &&
                !feedId.isBlank();
    }

    private boolean isValidStop(
            String stopId
    ) {

        return stopId != null &&
                !stopId.isBlank();
    }

    private int normalizeLimit(
            int limit
    ) {

        return Math.max(
                1,
                Math.min(
                        limit,
                        1000
                )
        );
    }
}