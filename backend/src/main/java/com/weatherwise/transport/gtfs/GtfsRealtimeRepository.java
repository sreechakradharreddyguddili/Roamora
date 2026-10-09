package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class GtfsRealtimeRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsRealtimeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceTripUpdates(
            String feedId,
            List<GtfsRealtimeTripUpdate> updates
    ) {
        deleteTripUpdates(feedId);

        if (updates == null || updates.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO gtfs_realtime_trip_update (
                    feed_id,
                    trip_id,
                    route_id,
                    stop_id,
                    stop_sequence,
                    scheduled_time,
                    estimated_time,
                    delay_seconds,
                    status,
                    fetched_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                updates,
                Math.min(500, updates.size()),
                (ps, update) -> {
                    ps.setString(1, update.feedId());
                    ps.setString(2, update.tripId());
                    ps.setString(3, update.routeId());
                    ps.setString(4, update.stopId());
                    ps.setObject(5, update.stopSequence());

                    if (update.scheduledTime() == null) {
                        ps.setTimestamp(6, null);
                    } else {
                        ps.setTimestamp(6, Timestamp.from(update.scheduledTime().toInstant()));
                    }

                    if (update.estimatedTime() == null) {
                        ps.setTimestamp(7, null);
                    } else {
                        ps.setTimestamp(7, Timestamp.from(update.estimatedTime().toInstant()));
                    }

                    ps.setObject(8, update.delaySeconds());
                    ps.setString(9, update.status());

                    OffsetDateTime fetchedAt = update.fetchedAt() == null
                            ? OffsetDateTime.now()
                            : update.fetchedAt();

                    ps.setTimestamp(
                            10,
                            Timestamp.from(fetchedAt.toInstant())
                    );
                }
        );
    }

    public void replaceVehicles(
            String feedId,
            List<GtfsRealtimeVehicle> vehicles
    ) {
        deleteVehicles(feedId);

        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO gtfs_realtime_vehicle (
                    feed_id,
                    vehicle_id,
                    trip_id,
                    route_id,
                    latitude,
                    longitude,
                    bearing,
                    speed,
                    current_stop_sequence,
                    current_status,
                    fetched_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                vehicles,
                Math.min(500, vehicles.size()),
                (ps, vehicle) -> {
                    ps.setString(1, vehicle.feedId());
                    ps.setString(2, vehicle.vehicleId());
                    ps.setString(3, vehicle.tripId());
                    ps.setString(4, vehicle.routeId());
                    ps.setObject(5, vehicle.latitude());
                    ps.setObject(6, vehicle.longitude());
                    ps.setObject(7, vehicle.bearing());
                    ps.setObject(8, vehicle.speed());
                    ps.setObject(9, vehicle.currentStopSequence());
                    ps.setString(10, vehicle.currentStatus());

                    OffsetDateTime fetchedAt = vehicle.fetchedAt() == null
                            ? OffsetDateTime.now()
                            : vehicle.fetchedAt();

                    ps.setTimestamp(
                            11,
                            Timestamp.from(fetchedAt.toInstant())
                    );
                }
        );
    }

    public void replaceAlerts(
            String feedId,
            List<GtfsRealtimeAlert> alerts
    ) {
        deleteAlerts(feedId);

        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO gtfs_realtime_alert (
                    feed_id,
                    alert_id,
                    cause,
                    effect,
                    header_text,
                    description_text,
                    url,
                    active_from,
                    active_until,
                    fetched_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                alerts,
                Math.min(250, alerts.size()),
                (ps, alert) -> {
                    ps.setString(1, alert.feedId());
                    ps.setString(2, alert.alertId());
                    ps.setString(3, alert.cause());
                    ps.setString(4, alert.effect());
                    ps.setString(5, alert.headerText());
                    ps.setString(6, alert.descriptionText());
                    ps.setString(7, alert.url());

                    if (alert.activeFrom() == null) {
                        ps.setTimestamp(8, null);
                    } else {
                        ps.setTimestamp(8, Timestamp.from(alert.activeFrom().toInstant()));
                    }

                    if (alert.activeUntil() == null) {
                        ps.setTimestamp(9, null);
                    } else {
                        ps.setTimestamp(9, Timestamp.from(alert.activeUntil().toInstant()));
                    }

                    OffsetDateTime fetchedAt = alert.fetchedAt() == null
                            ? OffsetDateTime.now()
                            : alert.fetchedAt();

                    ps.setTimestamp(
                            10,
                            Timestamp.from(fetchedAt.toInstant())
                    );
                }
        );
    }

    public List<GtfsRealtimeTripUpdate> findTripUpdates(
            String feedId,
            String tripId,
            String stopId,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    feed_id,
                    trip_id,
                    route_id,
                    stop_id,
                    stop_sequence,
                    scheduled_time,
                    estimated_time,
                    delay_seconds,
                    status,
                    fetched_at
                FROM gtfs_realtime_trip_update
                WHERE feed_id = ?
                """);

        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        params.add(feedId);

        if (tripId != null && !tripId.isBlank()) {
            sql.append(" AND trip_id = ?");
            params.add(tripId);
        }

        if (stopId != null && !stopId.isBlank()) {
            sql.append(" AND stop_id = ?");
            params.add(stopId);
        }

        sql.append(" ORDER BY COALESCE(estimated_time, scheduled_time) ASC LIMIT ?");
        params.add(safeLimit);

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> new GtfsRealtimeTripUpdate(
                        rs.getLong("id"),
                        rs.getString("feed_id"),
                        rs.getString("trip_id"),
                        rs.getString("route_id"),
                        rs.getString("stop_id"),
                        (Integer) rs.getObject("stop_sequence"),
                        toOffsetDateTime(rs.getTimestamp("scheduled_time")),
                        toOffsetDateTime(rs.getTimestamp("estimated_time")),
                        (Integer) rs.getObject("delay_seconds"),
                        rs.getString("status"),
                        toOffsetDateTime(rs.getTimestamp("fetched_at"))
                )
        );
    }

    public List<GtfsRealtimeVehicle> findVehicles(
            String feedId,
            String tripId,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    feed_id,
                    vehicle_id,
                    trip_id,
                    route_id,
                    latitude,
                    longitude,
                    bearing,
                    speed,
                    current_stop_sequence,
                    current_status,
                    fetched_at
                FROM gtfs_realtime_vehicle
                WHERE feed_id = ?
                """);

        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        params.add(feedId);

        if (tripId != null && !tripId.isBlank()) {
            sql.append(" AND trip_id = ?");
            params.add(tripId);
        }

        sql.append(" ORDER BY fetched_at DESC LIMIT ?");
        params.add(safeLimit);

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> new GtfsRealtimeVehicle(
                        rs.getLong("id"),
                        rs.getString("feed_id"),
                        rs.getString("vehicle_id"),
                        rs.getString("trip_id"),
                        rs.getString("route_id"),
                        (Double) rs.getObject("latitude"),
                        (Double) rs.getObject("longitude"),
                        (Double) rs.getObject("bearing"),
                        (Double) rs.getObject("speed"),
                        (Integer) rs.getObject("current_stop_sequence"),
                        rs.getString("current_status"),
                        toOffsetDateTime(rs.getTimestamp("fetched_at"))
                )
        );
    }

    public List<GtfsRealtimeAlert> findAlerts(
            String feedId,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 250));

        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    feed_id,
                    alert_id,
                    cause,
                    effect,
                    header_text,
                    description_text,
                    url,
                    active_from,
                    active_until,
                    fetched_at
                FROM gtfs_realtime_alert
                WHERE feed_id = ?
                  AND (
                      active_until IS NULL
                      OR active_until >= CURRENT_TIMESTAMP
                  )
                ORDER BY active_from NULLS FIRST, fetched_at DESC
                LIMIT ?
                """,
                new Object[]{feedId, safeLimit},
                (rs, rowNum) -> new GtfsRealtimeAlert(
                        rs.getLong("id"),
                        rs.getString("feed_id"),
                        rs.getString("alert_id"),
                        rs.getString("cause"),
                        rs.getString("effect"),
                        rs.getString("header_text"),
                        rs.getString("description_text"),
                        rs.getString("url"),
                        toOffsetDateTime(rs.getTimestamp("active_from")),
                        toOffsetDateTime(rs.getTimestamp("active_until")),
                        toOffsetDateTime(rs.getTimestamp("fetched_at"))
                )
        );
    }

    private void deleteTripUpdates(String feedId) {
        jdbcTemplate.update(
                "DELETE FROM gtfs_realtime_trip_update WHERE feed_id = ?",
                feedId
        );
    }

    private void deleteVehicles(String feedId) {
        jdbcTemplate.update(
                "DELETE FROM gtfs_realtime_vehicle WHERE feed_id = ?",
                feedId
        );
    }

    private void deleteAlerts(String feedId) {
        jdbcTemplate.update(
                "DELETE FROM gtfs_realtime_alert WHERE feed_id = ?",
                feedId
        );
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
    }
}
