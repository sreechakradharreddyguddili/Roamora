package com.weatherwise.transport.gtfs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gtfs/realtime")
public class GtfsRealtimeController {

    private final GtfsRealtimeService realtimeService;
    private final JdbcTemplate jdbcTemplate;

    public GtfsRealtimeController(
            GtfsRealtimeService realtimeService,
            JdbcTemplate jdbcTemplate
    ) {
        this.realtimeService = realtimeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/refresh/{feedId}")
    public GtfsRealtimeRefreshResult refresh(
            @PathVariable String feedId
    ) {
        GtfsFeed feed = findFeed(feedId);

        if (feed == null) {
            throw new IllegalArgumentException(
                    "GTFS feed not found: " + feedId
            );
        }

        return realtimeService.refreshFeed(feed);
    }

    @GetMapping("/trip-updates")
    public List<GtfsRealtimeTripUpdate> tripUpdates(
            @RequestParam String feedId,
            @RequestParam(required = false) String tripId,
            @RequestParam(required = false) String stopId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return realtimeService.findTripUpdates(
                feedId,
                tripId,
                stopId,
                limit
        );
    }

    @GetMapping("/vehicles")
    public List<GtfsRealtimeVehicle> vehicles(
            @RequestParam String feedId,
            @RequestParam(required = false) String tripId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return realtimeService.findVehicles(
                feedId,
                tripId,
                limit
        );
    }

    @GetMapping("/alerts")
    public List<GtfsRealtimeAlert> alerts(
            @RequestParam String feedId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return realtimeService.findAlerts(
                feedId,
                limit
        );
    }

    private GtfsFeed findFeed(String feedId) {

        List<GtfsFeed> feeds = jdbcTemplate.query(
                """
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
                LIMIT 1
                """,
                new Object[]{feedId},
                (rs, rowNum) -> {
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
                    feed.setRealtimeEnabled(
                            rs.getBoolean("realtime_enabled")
                    );

                    return feed;
                }
        );

        return feeds.isEmpty() ? null : feeds.get(0);
    }
}
