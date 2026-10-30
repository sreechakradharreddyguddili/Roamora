package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GtfsFeedRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsFeedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GtfsFeed> findAll(
            String country,
            String region,
            String city,
            Boolean active,
            Boolean realtimeEnabled
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
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
                FROM gtfs_feed
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        appendOptionalFilter(sql, params, "country", country);
        appendOptionalFilter(sql, params, "region", region);
        appendOptionalFilter(sql, params, "city", city);

        if (active != null) {
            sql.append(" AND active = ? ");
            params.add(active);
        }

        if (realtimeEnabled != null) {
            sql.append(" AND realtime_enabled = ? ");
            params.add(realtimeEnabled);
        }

        sql.append("""
                ORDER BY
                    country NULLS LAST,
                    region NULLS LAST,
                    city NULLS LAST,
                    provider_name NULLS LAST,
                    feed_id
                """);

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> mapFeed(rs)
        );
    }


    public List<GtfsFeed> findActiveStaticFeeds() {
        return findFeedsByFlags(true, false);
    }

    public List<GtfsFeed> findActiveRealtimeFeeds() {
        return findFeedsByFlags(true, true);
    }

    private List<GtfsFeed> findFeedsByFlags(
            boolean active,
            boolean realtimeEnabled
    ) {
        String sql = """
                SELECT
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
                FROM gtfs_feed
                WHERE active = ?
                  AND realtime_enabled = ?
                ORDER BY feed_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapFeed(rs),
                active,
                realtimeEnabled
        );
    }

    public GtfsFeed findById(String feedId) {
        if (feedId == null || feedId.isBlank()) {
            return null;
        }

        String sql = """
                SELECT
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
                FROM gtfs_feed
                WHERE feed_id = ?
                """;

        List<GtfsFeed> feeds = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapFeed(rs),
                feedId.trim()
        );

        return feeds.isEmpty() ? null : feeds.get(0);
    }

    public boolean exists(String feedId) {
        if (feedId == null || feedId.isBlank()) {
            return false;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gtfs_feed WHERE feed_id = ?",
                Integer.class,
                feedId.trim()
        );

        return count != null && count > 0;
    }

    private void appendOptionalFilter(
            StringBuilder sql,
            List<Object> params,
            String column,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        sql.append(" AND LOWER(TRIM(")
                .append(column)
                .append(")) = LOWER(TRIM(?)) ");

        params.add(value.trim());
    }

    private GtfsFeed mapFeed(
            ResultSet rs
    ) throws SQLException {

        GtfsFeed feed = new GtfsFeed();

        feed.setFeedId(rs.getString("feed_id"));
        feed.setProviderName(rs.getString("provider_name"));
        feed.setCountry(rs.getString("country"));
        feed.setCountryCode(rs.getString("country_code"));
        feed.setRegion(rs.getString("region"));
        feed.setCity(rs.getString("city"));
        feed.setFeedUrl(rs.getString("feed_url"));

        feed.setRealtimeTripUpdatesUrl(
                rs.getString("realtime_trip_updates_url")
        );

        feed.setRealtimeVehiclePositionsUrl(
                rs.getString("realtime_vehicle_positions_url")
        );

        feed.setRealtimeAlertsUrl(
                rs.getString("realtime_alerts_url")
        );

        feed.setLicenseUrl(rs.getString("license_url"));
        feed.setActive(rs.getBoolean("active"));
        feed.setRealtimeEnabled(rs.getBoolean("realtime_enabled"));

        return feed;
    }
}
