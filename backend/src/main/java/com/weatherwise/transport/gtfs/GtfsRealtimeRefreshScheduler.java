package com.weatherwise.transport.gtfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GtfsRealtimeRefreshScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(GtfsRealtimeRefreshScheduler.class);

    private final GtfsFeedService feedService;
    private final GtfsRealtimeService realtimeService;

    public GtfsRealtimeRefreshScheduler(
            GtfsFeedService feedService,
            GtfsRealtimeService realtimeService
    ) {
        this.feedService = feedService;
        this.realtimeService = realtimeService;
    }

    /**
     * Default refresh interval: 5 minutes.
     *
     * Can be configured with:
     * app.gtfs.realtime.refresh-interval-ms
     *
     * Initial delay can be configured with:
     * app.gtfs.realtime.refresh-initial-delay-ms
     */
    @Scheduled(
            fixedDelayString =
                    "${app.gtfs.realtime.refresh-interval-ms:300000}",
            initialDelayString =
                    "${app.gtfs.realtime.refresh-initial-delay-ms:30000}"
    )
    public void refreshAllFeeds() {

        List<GtfsFeed> feeds;

        try {
            feeds = feedService.findActiveRealtimeFeeds();
        } catch (Exception ex) {
            log.error(
                    "Unable to load active GTFS realtime feeds",
                    ex
            );
            return;
        }

        if (feeds == null || feeds.isEmpty()) {
            log.debug("No active GTFS realtime feeds configured");
            return;
        }

        log.info(
                "Starting GTFS realtime refresh for {} feed(s)",
                feeds.size()
        );

        int successful = 0;
        int failed = 0;

        for (GtfsFeed feed : feeds) {

            if (feed == null
                    || feed.getFeedId() == null
                    || feed.getFeedId().isBlank()) {

                failed++;
                log.warn(
                        "Skipping invalid GTFS realtime feed configuration"
                );
                continue;
            }

            try {
                GtfsRealtimeRefreshResult result =
                        realtimeService.refreshFeed(feed);

                successful++;

                log.info(
                        "GTFS realtime refreshed: feedId={}, tripUpdates={}, vehicles={}, alerts={}",
                        result.feedId(),
                        result.tripUpdateCount(),
                        result.vehicleCount(),
                        result.alertCount()
                );

            } catch (Exception ex) {
                failed++;

                log.error(
                        "GTFS realtime refresh failed: feedId={}",
                        feed.getFeedId(),
                        ex
                );
            }
        }

        log.info(
                "GTFS realtime refresh completed: total={}, successful={}, failed={}",
                feeds.size(),
                successful,
                failed
        );
    }
}
