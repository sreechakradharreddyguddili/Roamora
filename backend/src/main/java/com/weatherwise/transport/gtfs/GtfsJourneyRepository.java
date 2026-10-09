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
public class GtfsJourneyRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsJourneyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds direct same-trip journeys between two stops.
     *
     * This method does not apply a service-calendar date filter.
     * Use findJourneysForActiveServices(...) when planning for
     * a specific service date.
     */
    public List<GtfsJourney> findJourneys(
            String feedId,
            String originStopId,
            String destinationStopId,
            int limit
    ) {

        if (!isValidRequest(
                feedId,
                originStopId,
                destinationStopId
        )) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        String sql = """
                SELECT
                    f.feed_id,

                    os.stop_id AS origin_stop_id,
                    os.stop_name AS origin_stop_name,

                    ds.stop_id AS destination_stop_id,
                    ds.stop_name AS destination_stop_name,

                    t.trip_id,
                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    ot.departure_time AS origin_departure_time,
                    dt.arrival_time AS destination_arrival_time,

                    t.trip_headsign,
                    t.direction_id

                FROM gtfs_stop_time ot

                JOIN gtfs_trip t
                    ON t.feed_id = ot.feed_id
                    AND t.trip_id = ot.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop os
                    ON os.feed_id = ot.feed_id
                    AND os.stop_id = ot.stop_id

                JOIN gtfs_stop_time dt
                    ON dt.feed_id = ot.feed_id
                    AND dt.trip_id = ot.trip_id
                    AND dt.stop_sequence > ot.stop_sequence

                JOIN gtfs_stop ds
                    ON ds.feed_id = dt.feed_id
                    AND ds.stop_id = dt.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = ot.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE
                  AND ot.stop_id = ?
                  AND dt.stop_id = ?

                  AND ot.departure_time IS NOT NULL
                  AND dt.arrival_time IS NOT NULL

                ORDER BY
                    ot.departure_time ASC,
                    dt.stop_sequence ASC

                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, feedId);
                    ps.setString(2, originStopId);
                    ps.setString(3, destinationStopId);
                    ps.setInt(4, safeLimit);
                },
                (rs, rowNum) -> mapJourney(rs, feedId)
        );
    }

    /**
     * Finds direct journeys whose service_id belongs to the
     * services active on the requested date.
     */
    public List<GtfsJourney> findJourneysForActiveServices(
            String feedId,
            String originStopId,
            String destinationStopId,
            List<String> activeServiceIds,
            int limit
    ) {

        if (!isValidRequest(
                feedId,
                originStopId,
                destinationStopId
        )) {
            return List.of();
        }

        if (activeServiceIds == null ||
                activeServiceIds.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        List<String> validServiceIds =
                activeServiceIds.stream()
                        .filter(id ->
                                id != null &&
                                !id.isBlank()
                        )
                        .distinct()
                        .toList();

        if (validServiceIds.isEmpty()) {
            return List.of();
        }

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                validServiceIds.size(),
                                "?"
                        )
                );

        String sql = """
                SELECT
                    f.feed_id,

                    os.stop_id AS origin_stop_id,
                    os.stop_name AS origin_stop_name,

                    ds.stop_id AS destination_stop_id,
                    ds.stop_name AS destination_stop_name,

                    t.trip_id,
                    r.route_id,
                    r.route_short_name,
                    r.route_long_name,
                    r.route_type,

                    t.service_id,

                    ot.departure_time AS origin_departure_time,
                    dt.arrival_time AS destination_arrival_time,

                    t.trip_headsign,
                    t.direction_id

                FROM gtfs_stop_time ot

                JOIN gtfs_trip t
                    ON t.feed_id = ot.feed_id
                    AND t.trip_id = ot.trip_id

                JOIN gtfs_route r
                    ON r.feed_id = t.feed_id
                    AND r.route_id = t.route_id

                JOIN gtfs_stop os
                    ON os.feed_id = ot.feed_id
                    AND os.stop_id = ot.stop_id

                JOIN gtfs_stop_time dt
                    ON dt.feed_id = ot.feed_id
                    AND dt.trip_id = ot.trip_id
                    AND dt.stop_sequence > ot.stop_sequence

                JOIN gtfs_stop ds
                    ON ds.feed_id = dt.feed_id
                    AND ds.stop_id = dt.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = ot.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE
                  AND ot.stop_id = ?
                  AND dt.stop_id = ?

                  AND t.service_id IN (%s)

                  AND ot.departure_time IS NOT NULL
                  AND dt.arrival_time IS NOT NULL

                ORDER BY
                    ot.departure_time ASC,
                    dt.stop_sequence ASC

                LIMIT ?
                """.formatted(placeholders);

        Object[] parameters =
                new Object[
                        3 +
                        validServiceIds.size() +
                        1
                ];

        int index = 0;

        parameters[index++] = feedId;
        parameters[index++] = originStopId;
        parameters[index++] = destinationStopId;

        for (String serviceId : validServiceIds) {
            parameters[index++] = serviceId;
        }

        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                (rs, rowNum) ->
                        mapJourney(rs, feedId)
        );
    }

    /**
     * Finds journeys with exactly one transfer.
     *
     * Example:
     *
     * ORIGIN
     *    |
     *    | Trip 1
     *    v
     * TRANSFER STOP
     *    |
     *    | Trip 2
     *    v
     * DESTINATION
     *
     * The first and second legs may belong to different trips
     * and different routes.
     *
     * Example:
     *
     * BUS -> METRO
     * TRAIN -> BUS
     * TRAM -> TRAIN
     */
    public List<GtfsJourney> findOneTransferJourneysForActiveServices(
            String feedId,
            String originStopId,
            String destinationStopId,
            List<String> activeServiceIds,
            int minimumTransferMinutes,
            int limit
    ) {

        if (!isValidRequest(
                feedId,
                originStopId,
                destinationStopId
        )) {
            return List.of();
        }

        if (activeServiceIds == null ||
                activeServiceIds.isEmpty()) {
            return List.of();
        }

        List<String> validServiceIds =
                activeServiceIds.stream()
                        .filter(id ->
                                id != null &&
                                !id.isBlank()
                        )
                        .distinct()
                        .toList();

        if (validServiceIds.isEmpty()) {
            return List.of();
        }

        int safeTransferMinutes =
                Math.max(
                        0,
                        Math.min(
                                minimumTransferMinutes,
                                180
                        )
                );

        int safeLimit = normalizeLimit(limit);

        String placeholders =
                String.join(
                        ",",
                        Collections.nCopies(
                                validServiceIds.size(),
                                "?"
                        )
                );

        /*
         * We use the same stop as the transfer point.
         *
         * First leg:
         *
         *   origin -> transfer
         *
         * Second leg:
         *
         *   transfer -> destination
         *
         * The second departure must occur after the first
         * arrival plus the minimum transfer time.
         */
        String sql = """
                SELECT
                    f.feed_id,

                    origin_stop.stop_id
                        AS origin_stop_id,

                    origin_stop.stop_name
                        AS origin_stop_name,

                    transfer_stop.stop_id
                        AS transfer_stop_id,

                    transfer_stop.stop_name
                        AS transfer_stop_name,

                    destination_stop.stop_id
                        AS destination_stop_id,

                    destination_stop.stop_name
                        AS destination_stop_name,

                    /*
                     * First leg
                     */
                    trip1.trip_id
                        AS first_trip_id,

                    route1.route_id
                        AS first_route_id,

                    route1.route_short_name
                        AS first_route_short_name,

                    route1.route_long_name
                        AS first_route_long_name,

                    route1.route_type
                        AS first_route_type,

                    trip1.service_id
                        AS first_service_id,

                    first_origin.departure_time
                        AS first_departure_time,

                    first_transfer.arrival_time
                        AS first_arrival_time,

                    trip1.trip_headsign
                        AS first_trip_headsign,

                    trip1.direction_id
                        AS first_direction_id,

                    /*
                     * Second leg
                     */
                    trip2.trip_id
                        AS second_trip_id,

                    route2.route_id
                        AS second_route_id,

                    route2.route_short_name
                        AS second_route_short_name,

                    route2.route_long_name
                        AS second_route_long_name,

                    route2.route_type
                        AS second_route_type,

                    trip2.service_id
                        AS second_service_id,

                    second_transfer.departure_time
                        AS second_departure_time,

                    second_destination.arrival_time
                        AS second_arrival_time,

                    trip2.trip_headsign
                        AS second_trip_headsign,

                    trip2.direction_id
                        AS second_direction_id

                FROM gtfs_stop_time first_origin

                JOIN gtfs_trip trip1
                    ON trip1.feed_id = first_origin.feed_id
                    AND trip1.trip_id = first_origin.trip_id

                JOIN gtfs_route route1
                    ON route1.feed_id = trip1.feed_id
                    AND route1.route_id = trip1.route_id

                JOIN gtfs_stop origin_stop
                    ON origin_stop.feed_id = first_origin.feed_id
                    AND origin_stop.stop_id = first_origin.stop_id

                JOIN gtfs_stop_time first_transfer
                    ON first_transfer.feed_id = first_origin.feed_id
                    AND first_transfer.trip_id = first_origin.trip_id
                    AND first_transfer.stop_sequence >
                        first_origin.stop_sequence

                JOIN gtfs_stop transfer_stop
                    ON transfer_stop.feed_id = first_transfer.feed_id
                    AND transfer_stop.stop_id = first_transfer.stop_id

                JOIN gtfs_stop_time second_transfer
                    ON second_transfer.feed_id = first_transfer.feed_id
                    AND second_transfer.stop_id = first_transfer.stop_id

                JOIN gtfs_trip trip2
                    ON trip2.feed_id = second_transfer.feed_id
                    AND trip2.trip_id = second_transfer.trip_id

                JOIN gtfs_route route2
                    ON route2.feed_id = trip2.feed_id
                    AND route2.route_id = trip2.route_id

                JOIN gtfs_stop_time second_destination
                    ON second_destination.feed_id = second_transfer.feed_id
                    AND second_destination.trip_id = second_transfer.trip_id
                    AND second_destination.stop_sequence >
                        second_transfer.stop_sequence

                JOIN gtfs_stop destination_stop
                    ON destination_stop.feed_id =
                        second_destination.feed_id
                    AND destination_stop.stop_id =
                        second_destination.stop_id

                JOIN gtfs_feed f
                    ON f.feed_id = first_origin.feed_id

                WHERE f.feed_id = ?
                  AND f.active = TRUE

                  AND first_origin.stop_id = ?
                  AND destination_stop.stop_id = ?

                  AND first_origin.departure_time IS NOT NULL
                  AND first_transfer.arrival_time IS NOT NULL

                  AND second_transfer.departure_time IS NOT NULL
                  AND second_destination.arrival_time IS NOT NULL

                  /*
                   * Both trips must operate on the requested
                   * service date.
                   */
                  AND trip1.service_id IN (%s)
                  AND trip2.service_id IN (%s)

                  /*
                   * Do not allow the second trip to be the
                   * exact same trip.
                   */
                  AND trip1.trip_id <> trip2.trip_id

                  /*
                   * The second departure must leave after
                   * the first leg arrives plus the required
                   * transfer time.
                   */
                  AND (
                      (
                          split_part(
                              first_transfer.arrival_time,
                              ':',
                              1
                          )::INTEGER * 3600
                          +
                          split_part(
                              first_transfer.arrival_time,
                              ':',
                              2
                          )::INTEGER * 60
                          +
                          split_part(
                              first_transfer.arrival_time,
                              ':',
                              3
                          )::INTEGER
                      )
                      +
                      (? * 60)
                  )
                  <=
                  (
                      split_part(
                          second_transfer.departure_time,
                          ':',
                          1
                      )::INTEGER * 3600
                      +
                      split_part(
                          second_transfer.departure_time,
                          ':',
                          2
                      )::INTEGER * 60
                      +
                      split_part(
                          second_transfer.departure_time,
                          ':',
                          3
                      )::INTEGER
                  )

                ORDER BY
                    first_origin.departure_time ASC,
                    second_destination.arrival_time ASC

                LIMIT ?
                """.formatted(
                        placeholders,
                        placeholders
                );

        Object[] parameters =
                new Object[
                        3 +
                        validServiceIds.size() +
                        validServiceIds.size() +
                        2
                ];

        int index = 0;

        parameters[index++] = feedId;
        parameters[index++] = originStopId;
        parameters[index++] = destinationStopId;

        for (String serviceId : validServiceIds) {
            parameters[index++] = serviceId;
        }

        for (String serviceId : validServiceIds) {
            parameters[index++] = serviceId;
        }

        parameters[index++] = safeTransferMinutes;
        parameters[index] = safeLimit;

        return jdbcTemplate.query(
                sql,
                parameters,
                (rs, rowNum) ->
                        mapOneTransferJourney(rs, feedId)
        );
    }

    /**
     * Maps a direct same-trip journey.
     */
    private GtfsJourney mapJourney(
            ResultSet rs,
            String feedId
    ) throws SQLException {

        String originDeparture =
                rs.getString(
                        "origin_departure_time"
                );

        String destinationArrival =
                rs.getString(
                        "destination_arrival_time"
                );

        LocalDateTime departureTime =
                parseGtfsDateTime(
                        originDeparture
                );

        LocalDateTime arrivalTime =
                parseGtfsDateTime(
                        destinationArrival
                );

        long durationMinutes = 0;

        if (departureTime != null &&
                arrivalTime != null) {

            durationMinutes =
                    Duration.between(
                            departureTime,
                            arrivalTime
                    ).toMinutes();

            if (durationMinutes < 0) {
                durationMinutes = 0;
            }
        }

        String destination =
                rs.getString(
                        "trip_headsign"
                );

        if (destination == null ||
                destination.isBlank()) {

            destination =
                    rs.getString(
                            "destination_stop_name"
                    );
        }

        GtfsJourneyLeg leg =
                new GtfsJourneyLeg(
                        feedId,

                        rs.getString(
                                "origin_stop_id"
                        ),

                        rs.getString(
                                "origin_stop_name"
                        ),

                        rs.getString(
                                "destination_stop_id"
                        ),

                        rs.getString(
                                "destination_stop_name"
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

                        Duration.ofMinutes(
                                durationMinutes
                        ),

                        null,

                        destination,

                        "SCHEDULED"
                );

        return new GtfsJourney(
                feedId,

                rs.getString(
                        "origin_stop_id"
                ),

                rs.getString(
                        "origin_stop_name"
                ),

                rs.getString(
                        "destination_stop_id"
                ),

                rs.getString(
                        "destination_stop_name"
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

                Duration.ofMinutes(
                        durationMinutes
                ),

                0,

                destination,

                "SCHEDULED",

                List.of(leg),

                Duration.ZERO,

                Duration.ZERO
        );
    }

    /**
     * Maps a two-leg journey:
     *
     * origin -> transfer -> destination
     */
    private GtfsJourney mapOneTransferJourney(
            ResultSet rs,
            String feedId
    ) throws SQLException {

        LocalDateTime firstDeparture =
                parseGtfsDateTime(
                        rs.getString(
                                "first_departure_time"
                        )
                );

        LocalDateTime firstArrival =
                parseGtfsDateTime(
                        rs.getString(
                                "first_arrival_time"
                        )
                );

        LocalDateTime secondDeparture =
                parseGtfsDateTime(
                        rs.getString(
                                "second_departure_time"
                        )
                );

        LocalDateTime secondArrival =
                parseGtfsDateTime(
                        rs.getString(
                                "second_arrival_time"
                        )
                );

        long firstDuration =
                durationMinutes(
                        firstDeparture,
                        firstArrival
                );

        long secondDuration =
                durationMinutes(
                        secondDeparture,
                        secondArrival
                );

        long waitingMinutes =
                durationMinutes(
                        firstArrival,
                        secondDeparture
                );

        long totalDuration =
                durationMinutes(
                        firstDeparture,
                        secondArrival
                );

        if (totalDuration < 0) {
            totalDuration = 0;
        }

        if (waitingMinutes < 0) {
            waitingMinutes = 0;
        }

        String firstDestination =
                rs.getString(
                        "first_trip_headsign"
                );

        if (firstDestination == null ||
                firstDestination.isBlank()) {

            firstDestination =
                    rs.getString(
                            "transfer_stop_name"
                    );
        }

        String secondDestination =
                rs.getString(
                        "second_trip_headsign"
                );

        if (secondDestination == null ||
                secondDestination.isBlank()) {

            secondDestination =
                    rs.getString(
                            "destination_stop_name"
                    );
        }

        GtfsJourneyLeg firstLeg =
                new GtfsJourneyLeg(
                        feedId,

                        rs.getString(
                                "origin_stop_id"
                        ),

                        rs.getString(
                                "origin_stop_name"
                        ),

                        rs.getString(
                                "transfer_stop_id"
                        ),

                        rs.getString(
                                "transfer_stop_name"
                        ),

                        rs.getString(
                                "first_trip_id"
                        ),

                        rs.getString(
                                "first_route_id"
                        ),

                        rs.getString(
                                "first_route_short_name"
                        ),

                        rs.getString(
                                "first_route_long_name"
                        ),

                        rs.getString(
                                "first_route_type"
                        ),

                        rs.getString(
                                "first_service_id"
                        ),

                        firstDeparture,
                        firstArrival,

                        Duration.ofMinutes(
                                firstDuration
                        ),

                        null,

                        firstDestination,

                        "SCHEDULED"
                );

        GtfsJourneyLeg secondLeg =
                new GtfsJourneyLeg(
                        feedId,

                        rs.getString(
                                "transfer_stop_id"
                        ),

                        rs.getString(
                                "transfer_stop_name"
                        ),

                        rs.getString(
                                "destination_stop_id"
                        ),

                        rs.getString(
                                "destination_stop_name"
                        ),

                        rs.getString(
                                "second_trip_id"
                        ),

                        rs.getString(
                                "second_route_id"
                        ),

                        rs.getString(
                                "second_route_short_name"
                        ),

                        rs.getString(
                                "second_route_long_name"
                        ),

                        rs.getString(
                                "second_route_type"
                        ),

                        rs.getString(
                                "second_service_id"
                        ),

                        secondDeparture,
                        secondArrival,

                        Duration.ofMinutes(
                                secondDuration
                        ),

                        null,

                        secondDestination,

                        "SCHEDULED"
                );

        String destination =
                secondDestination;

        /*
         * Keep the existing top-level fields useful for
         * backward compatibility.
         *
         * The top-level trip/route represent the first leg.
         * The complete journey is available through legs.
         */
        return new GtfsJourney(
                feedId,

                rs.getString(
                        "origin_stop_id"
                ),

                rs.getString(
                        "origin_stop_name"
                ),

                rs.getString(
                        "destination_stop_id"
                ),

                rs.getString(
                        "destination_stop_name"
                ),

                rs.getString(
                        "first_trip_id"
                ),

                rs.getString(
                        "first_route_id"
                ),

                rs.getString(
                        "first_route_short_name"
                ),

                rs.getString(
                        "first_route_long_name"
                ),

                rs.getString(
                        "first_route_type"
                ),

                rs.getString(
                        "first_service_id"
                ),

                firstDeparture,
                secondArrival,

                Duration.ofMinutes(
                        totalDuration
                ),

                1,

                destination,

                "SCHEDULED",

                List.of(
                        firstLeg,
                        secondLeg
                ),

                Duration.ofMinutes(
                        waitingMinutes
                ),

                Duration.ofMinutes(
                        waitingMinutes
                )
        );
    }

    /**
     * Calculates duration in minutes.
     */
    private long durationMinutes(
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (start == null ||
                end == null) {
            return 0;
        }

        long minutes =
                Duration.between(
                        start,
                        end
                ).toMinutes();

        return Math.max(
                0,
                minutes
        );
    }

    /**
     * Parses GTFS time values.
     *
     * Supports:
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

            if (minute < 0 ||
                    minute > 59 ||
                    second < 0 ||
                    second > 59 ||
                    hour < 0) {

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

    /**
     * Validates journey requests.
     */
    private boolean isValidRequest(
            String feedId,
            String originStopId,
            String destinationStopId
    ) {

        if (feedId == null ||
                feedId.isBlank()) {
            return false;
        }

        if (originStopId == null ||
                originStopId.isBlank()) {
            return false;
        }

        if (destinationStopId == null ||
                destinationStopId.isBlank()) {
            return false;
        }

        return !originStopId.equals(
                destinationStopId
        );
    }

    /**
     * Keeps repository result sizes bounded.
     */
    private int normalizeLimit(
            int limit
    ) {

        return Math.max(
                1,
                Math.min(
                        limit,
                        100
                )
        );
    }
}