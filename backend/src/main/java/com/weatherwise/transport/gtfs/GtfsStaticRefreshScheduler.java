package com.weatherwise.transport.gtfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GtfsStaticRefreshScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GtfsStaticRefreshScheduler.class
            );

    private final GtfsFeedRepository feedRepository;
    private final GtfsIngestionService ingestionService;

    public GtfsStaticRefreshScheduler(
            GtfsFeedRepository feedRepository,
            GtfsIngestionService ingestionService
    ) {
        this.feedRepository = feedRepository;
        this.ingestionService = ingestionService;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.gtfs.static.refresh-interval-ms:86400000}",
            initialDelayString =
                    "${app.gtfs.static.refresh-initial-delay-ms:60000}"
    )
    public void refreshAllStaticFeeds() {

        List<GtfsFeed> feeds;

        try {
            feeds = feedRepository.findActiveStaticFeeds();
        } catch (Exception ex) {
            log.error(
                    "Unable to load active GTFS static feeds",
                    ex
            );
            return;
        }

        if (feeds == null || feeds.isEmpty()) {
            log.debug(
                    "No active GTFS static feeds configured"
            );
            return;
        }

        log.info(
                "Starting GTFS static refresh for {} feed(s)",
                feeds.size()
        );

        for (GtfsFeed feed : feeds) {

            if (feed == null
                    || feed.getFeedId() == null
                    || feed.getFeedId().isBlank()) {
                continue;
            }

            try {
                long start = System.currentTimeMillis();

                log.info(
                        "Refreshing GTFS feed {} ({}, {}, {})",
                        feed.getFeedId(),
                        feed.getCountry(),
                        feed.getRegion(),
                        feed.getCity()
                );

                ingestionService.ingest(feed);

                log.info(
                        "GTFS static refresh completed for {} in {} ms",
                        feed.getFeedId(),
                        System.currentTimeMillis() - start
                );

            } catch (Exception ex) {
                /*
                 * One bad worldwide feed must not prevent the
                 * remaining feeds from refreshing.
                 */
                log.error(
                        "GTFS static refresh failed for feed {}",
                        feed.getFeedId(),
                        ex
                );
            }
        }

        log.info("GTFS static refresh cycle completed");
    }
}
