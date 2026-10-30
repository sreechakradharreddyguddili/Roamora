package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class GtfsRepository {

    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    public GtfsRepository(
            JdbcTemplate jdbcTemplate
    ) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    // ============================================================
    // FEED
    // ============================================================

    public void saveFeed(
            GtfsFeed feed
    ) {

        jdbcTemplate.update(
                """
                INSERT INTO gtfs_feed (
                    feed_id,
                    provider_name,
                    country,
                    country_code,
                    region,
                    city,
                    feed_url,
                    realtime_trip_updates_url,
                    realtime_vehicle_positions_url,
                    realtime_alerts_url,
                    license_url,
                    active,
                    realtime_enabled
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

                ON CONFLICT (feed_id)
                DO UPDATE SET
                    provider_name =
                        EXCLUDED.provider_name,

                    country =
                        EXCLUDED.country,

                    country_code =
                        EXCLUDED.country_code,

                    region =
                        EXCLUDED.region,

                    city =
                        EXCLUDED.city,

                    feed_url =
                        EXCLUDED.feed_url,

                    realtime_trip_updates_url =
                        EXCLUDED.realtime_trip_updates_url,

                    realtime_vehicle_positions_url =
                        EXCLUDED.realtime_vehicle_positions_url,

                    realtime_alerts_url =
                        EXCLUDED.realtime_alerts_url,

                    license_url =
                        EXCLUDED.license_url,

                    active =
                        EXCLUDED.active,

                    realtime_enabled =
                        EXCLUDED.realtime_enabled,

                    updated_at =
                        CURRENT_TIMESTAMP
                """,

                feed.getFeedId(),
                feed.getProviderName(),
                feed.getCountry(),
                feed.getCountryCode(),
                feed.getRegion(),
                feed.getCity(),
                feed.getFeedUrl(),
                feed.getRealtimeTripUpdatesUrl(),
                feed.getRealtimeVehiclePositionsUrl(),
                feed.getRealtimeAlertsUrl(),
                feed.getLicenseUrl(),
                feed.isActive(),
                feed.isRealtimeEnabled()
        );
    }

    // ============================================================
    // STOPS
    // ============================================================


    /**
     * Removes the previous static GTFS snapshot for a feed.
     *
     * The delete order follows the foreign-key relationships:
     * stop times -> trips -> routes/stops and calendar data.
     *
     * Realtime tables are intentionally not touched here because
     * realtime data belongs to the separate GTFS-RT refresh cycle.
     */
    public void clearStaticFeedData(String feedId) {

        if (feedId == null || feedId.isBlank()) {
            throw new IllegalArgumentException(
                    "GTFS feed ID must not be blank."
            );
        }

        String normalizedFeedId = feedId.trim();

        jdbcTemplate.update(
                "DELETE FROM gtfs_stop_time WHERE feed_id = ?",
                normalizedFeedId
        );

        jdbcTemplate.update(
                "DELETE FROM gtfs_calendar_date WHERE feed_id = ?",
                normalizedFeedId
        );

        jdbcTemplate.update(
                "DELETE FROM gtfs_calendar WHERE feed_id = ?",
                normalizedFeedId
        );

        jdbcTemplate.update(
                "DELETE FROM gtfs_trip WHERE feed_id = ?",
                normalizedFeedId
        );

        jdbcTemplate.update(
                "DELETE FROM gtfs_route WHERE feed_id = ?",
                normalizedFeedId
        );

        jdbcTemplate.update(
                "DELETE FROM gtfs_stop WHERE feed_id = ?",
                normalizedFeedId
        );
    }

    public List<GtfsStop> searchStops(
            String feedId,
            String query,
            int limit
    ) {
        String normalizedQuery = query == null ? "" : query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        if (normalizedQuery.length() < 2) return List.of();

        return jdbcTemplate.query(
                """
                SELECT stop_id, stop_code, stop_name, latitude, longitude,
                       location_type, parent_station, timezone
                FROM gtfs_stop
                WHERE feed_id = ?
                  AND LOWER(stop_name) LIKE LOWER(?)
                ORDER BY CASE WHEN LOWER(stop_name) = LOWER(?) THEN 0 ELSE 1 END, stop_name
                LIMIT ?
                """,
                (rs, rowNum) -> new GtfsStop(
                        rs.getString("stop_id"),
                        rs.getString("stop_code"),
                        rs.getString("stop_name"),
                        (Double) rs.getObject("latitude"),
                        (Double) rs.getObject("longitude"),
                        rs.getString("location_type"),
                        rs.getString("parent_station"),
                        rs.getString("timezone")
                ),
                feedId.trim(),
                "%" + normalizedQuery + "%",
                normalizedQuery,
                safeLimit
        );
    }

    public void saveStops(
            String feedId,
            List<GtfsStop> stops
    ) {

        if (stops == null
                || stops.isEmpty()) {

            return;
        }

        String sql =
                """
                INSERT INTO gtfs_stop (
                    feed_id,
                    stop_id,
                    stop_code,
                    stop_name,
                    latitude,
                    longitude,
                    location_type,
                    parent_station,
                    timezone
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)

                ON CONFLICT (feed_id, stop_id)
                DO UPDATE SET
                    stop_code =
                        EXCLUDED.stop_code,

                    stop_name =
                        EXCLUDED.stop_name,

                    latitude =
                        EXCLUDED.latitude,

                    longitude =
                        EXCLUDED.longitude,

                    location_type =
                        EXCLUDED.location_type,

                    parent_station =
                        EXCLUDED.parent_station,

                    timezone =
                        EXCLUDED.timezone
                """;

        batch(
                stops,
                sql,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        GtfsStop stop =
                                stops.get(index);

                        ps.setString(
                                1,
                                feedId
                        );

                        ps.setString(
                                2,
                                stop.stopId()
                        );

                        ps.setString(
                                3,
                                stop.stopCode()
                        );

                        ps.setString(
                                4,
                                stop.stopName()
                        );

                        ps.setObject(
                                5,
                                stop.latitude()
                        );

                        ps.setObject(
                                6,
                                stop.longitude()
                        );

                        ps.setString(
                                7,
                                stop.locationType()
                        );

                        ps.setString(
                                8,
                                stop.parentStation()
                        );

                        ps.setString(
                                9,
                                stop.timezone()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return stops.size();
                    }
                }
        );
    }

    // ============================================================
    // ROUTES
    // ============================================================

    public void saveRoutes(
            String feedId,
            List<GtfsRoute> routes
    ) {

        if (routes == null
                || routes.isEmpty()) {

            return;
        }

        String sql =
                """
                INSERT INTO gtfs_route (
                    feed_id,
                    route_id,
                    route_short_name,
                    route_long_name,
                    route_type,
                    agency_id
                )
                VALUES (?, ?, ?, ?, ?, ?)

                ON CONFLICT (feed_id, route_id)
                DO UPDATE SET
                    route_short_name =
                        EXCLUDED.route_short_name,

                    route_long_name =
                        EXCLUDED.route_long_name,

                    route_type =
                        EXCLUDED.route_type,

                    agency_id =
                        EXCLUDED.agency_id
                """;

        batch(
                routes,
                sql,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        GtfsRoute route =
                                routes.get(index);

                        ps.setString(
                                1,
                                feedId
                        );

                        ps.setString(
                                2,
                                route.routeId()
                        );

                        ps.setString(
                                3,
                                route.routeShortName()
                        );

                        ps.setString(
                                4,
                                route.routeLongName()
                        );

                        ps.setString(
                                5,
                                route.routeType()
                        );

                        ps.setString(
                                6,
                                route.agencyId()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return routes.size();
                    }
                }
        );
    }

    // ============================================================
    // TRIPS
    // ============================================================

    public void saveTrips(
            String feedId,
            List<GtfsTrip> trips
    ) {

        if (trips == null
                || trips.isEmpty()) {

            return;
        }

        String sql =
                """
                INSERT INTO gtfs_trip (
                    feed_id,
                    trip_id,
                    route_id,
                    service_id,
                    trip_headsign,
                    direction_id
                )
                VALUES (?, ?, ?, ?, ?, ?)

                ON CONFLICT (feed_id, trip_id)
                DO UPDATE SET
                    route_id =
                        EXCLUDED.route_id,

                    service_id =
                        EXCLUDED.service_id,

                    trip_headsign =
                        EXCLUDED.trip_headsign,

                    direction_id =
                        EXCLUDED.direction_id
                """;

        batch(
                trips,
                sql,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        GtfsTrip trip =
                                trips.get(index);

                        ps.setString(
                                1,
                                feedId
                        );

                        ps.setString(
                                2,
                                trip.tripId()
                        );

                        ps.setString(
                                3,
                                trip.routeId()
                        );

                        ps.setString(
                                4,
                                trip.serviceId()
                        );

                        ps.setString(
                                5,
                                trip.tripHeadsign()
                        );

                        ps.setString(
                                6,
                                trip.directionId()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return trips.size();
                    }
                }
        );
    }

    // ============================================================
    // CALENDAR
    // ============================================================

    public void saveCalendars(
            String feedId,
            List<GtfsCalendar> calendars
    ) {

        /*
         * Clear only this feed.
         *
         * This makes re-ingestion safe.
         */
        jdbcTemplate.update(
                """
                DELETE FROM gtfs_calendar
                WHERE feed_id = ?
                """,
                feedId
        );

        if (calendars == null
                || calendars.isEmpty()) {

            return;
        }

        String sql =
                """
                INSERT INTO gtfs_calendar (
                    feed_id,
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
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

                ON CONFLICT (
                    feed_id,
                    service_id
                )
                DO UPDATE SET
                    monday =
                        EXCLUDED.monday,

                    tuesday =
                        EXCLUDED.tuesday,

                    wednesday =
                        EXCLUDED.wednesday,

                    thursday =
                        EXCLUDED.thursday,

                    friday =
                        EXCLUDED.friday,

                    saturday =
                        EXCLUDED.saturday,

                    sunday =
                        EXCLUDED.sunday,

                    start_date =
                        EXCLUDED.start_date,

                    end_date =
                        EXCLUDED.end_date
                """;

        batch(
                calendars,
                sql,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        GtfsCalendar calendar =
                                calendars.get(index);

                        ps.setString(
                                1,
                                feedId
                        );

                        ps.setString(
                                2,
                                calendar.serviceId()
                        );

                        ps.setBoolean(
                                3,
                                calendar.monday()
                        );

                        ps.setBoolean(
                                4,
                                calendar.tuesday()
                        );

                        ps.setBoolean(
                                5,
                                calendar.wednesday()
                        );

                        ps.setBoolean(
                                6,
                                calendar.thursday()
                        );

                        ps.setBoolean(
                                7,
                                calendar.friday()
                        );

                        ps.setBoolean(
                                8,
                                calendar.saturday()
                        );

                        ps.setBoolean(
                                9,
                                calendar.sunday()
                        );

                        ps.setObject(
                                10,
                                calendar.startDate()
                        );

                        ps.setObject(
                                11,
                                calendar.endDate()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return calendars.size();
                    }
                }
        );
    }

    // ============================================================
    // CALENDAR DATES
    // ============================================================

    public void saveCalendarDates(
            String feedId,
            List<GtfsCalendarDate> calendarDates
    ) {

        /*
         * Clear previous exceptions for this feed.
         */
        jdbcTemplate.update(
                """
                DELETE FROM gtfs_calendar_date
                WHERE feed_id = ?
                """,
                feedId
        );

        if (calendarDates == null
                || calendarDates.isEmpty()) {

            return;
        }

        String sql =
                """
                INSERT INTO gtfs_calendar_date (
                    feed_id,
                    service_id,
                    exception_date,
                    exception_type
                )
                VALUES (?, ?, ?, ?)

                ON CONFLICT (
                    feed_id,
                    service_id,
                    exception_date
                )
                DO UPDATE SET
                    exception_type =
                        EXCLUDED.exception_type
                """;

        batch(
                calendarDates,
                sql,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        GtfsCalendarDate calendarDate =
                                calendarDates.get(index);

                        ps.setString(
                                1,
                                feedId
                        );

                        ps.setString(
                                2,
                                calendarDate.serviceId()
                        );

                        ps.setObject(
                                3,
                                calendarDate.exceptionDate()
                        );

                        ps.setInt(
                                4,
                                calendarDate.exceptionType()
                        );
                    }

                    @Override
                    public int getBatchSize() {
                        return calendarDates.size();
                    }
                }
        );
    }

    // ============================================================
    // STOP TIMES
    // ============================================================

    public void saveStopTimes(
            String feedId,
            List<GtfsStopTime> stopTimes
    ) {

        if (stopTimes == null
                || stopTimes.isEmpty()) {

            return;
        }

        /*
         * GTFS stop_times has no single-column ID.
         *
         * Delete the existing feed data before loading
         * the latest timetable.
         *
         * GtfsIngestionService is transactional.
         */
        jdbcTemplate.update(
                """
                DELETE FROM gtfs_stop_time
                WHERE feed_id = ?
                """,
                feedId
        );

        String sql =
                """
                INSERT INTO gtfs_stop_time (
                    feed_id,
                    trip_id,
                    stop_id,
                    arrival_time,
                    departure_time,
                    stop_sequence,
                    stop_headsign
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        for (
                int start = 0;
                start < stopTimes.size();
                start += BATCH_SIZE
        ) {

            int end =
                    Math.min(
                            start + BATCH_SIZE,
                            stopTimes.size()
                    );

            List<GtfsStopTime> currentBatch =
                    stopTimes.subList(
                            start,
                            end
                    );

            jdbcTemplate.batchUpdate(
                    sql,
                    new BatchPreparedStatementSetter() {

                        @Override
                        public void setValues(
                                PreparedStatement ps,
                                int index
                        ) throws SQLException {

                            GtfsStopTime stopTime =
                                    currentBatch.get(index);

                            ps.setString(
                                    1,
                                    feedId
                            );

                            ps.setString(
                                    2,
                                    stopTime.tripId()
                            );

                            ps.setString(
                                    3,
                                    stopTime.stopId()
                            );

                            ps.setString(
                                    4,
                                    stopTime.arrivalTime()
                            );

                            ps.setString(
                                    5,
                                    stopTime.departureTime()
                            );

                            ps.setObject(
                                    6,
                                    stopTime.stopSequence()
                            );

                            ps.setString(
                                    7,
                                    stopTime.stopHeadsign()
                            );
                        }

                        @Override
                        public int getBatchSize() {
                            return currentBatch.size();
                        }
                    }
            );
        }
    }

    // ============================================================
    // SINGLE RECORD METHODS
    // ============================================================

    public void saveStop(
            String feedId,
            GtfsStop stop
    ) {

        saveStops(
                feedId,
                List.of(stop)
        );
    }

    public void saveRoute(
            String feedId,
            GtfsRoute route
    ) {

        saveRoutes(
                feedId,
                List.of(route)
        );
    }

    public void saveTrip(
            String feedId,
            GtfsTrip trip
    ) {

        saveTrips(
                feedId,
                List.of(trip)
        );
    }

    public void saveStopTime(
            String feedId,
            GtfsStopTime stopTime
    ) {

        saveStopTimes(
                feedId,
                List.of(stopTime)
        );
    }

    // ============================================================
    // GENERIC BATCH HELPER
    // ============================================================

    private <T> void batch(
            List<T> records,
            String sql,
            BatchPreparedStatementSetter setter
    ) {

        for (
                int start = 0;
                start < records.size();
                start += BATCH_SIZE
        ) {

            final int batchStart =
                    start;

            int end =
                    Math.min(
                            batchStart + BATCH_SIZE,
                            records.size()
                    );

            List<T> currentBatch =
                    records.subList(
                            batchStart,
                            end
                    );

            jdbcTemplate.batchUpdate(
                    sql,
                    new BatchPreparedStatementSetter() {

                        @Override
                        public void setValues(
                                PreparedStatement ps,
                                int index
                        ) throws SQLException {

                            setter.setValues(
                                    ps,
                                    batchStart + index
                            );
                        }

                        @Override
                        public int getBatchSize() {
                            return currentBatch.size();
                        }
                    }
            );
        }
    }
}