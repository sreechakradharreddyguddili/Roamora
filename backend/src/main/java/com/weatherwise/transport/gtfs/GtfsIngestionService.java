package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GtfsIngestionService {

    private final GtfsFeedLoader feedLoader;
    private final GtfsRepository repository;

    public GtfsIngestionService(
            GtfsFeedLoader feedLoader,
            GtfsRepository repository
    ) {
        this.feedLoader = feedLoader;
        this.repository = repository;
    }

    @Transactional
    public void ingest(GtfsFeed feed) {

        if (feed == null) {
            throw new IllegalArgumentException(
                    "GTFS feed must not be null."
            );
        }

        if (feed.getFeedId() == null
                || feed.getFeedId().isBlank()) {
            throw new IllegalArgumentException(
                    "GTFS feed ID must not be blank."
            );
        }

        if (!feed.isActive()) {
            throw new IllegalStateException(
                    "GTFS feed is inactive: "
                            + feed.getFeedId()
            );
        }

        if (feed.getFeedUrl() == null
                || feed.getFeedUrl().isBlank()) {
            throw new IllegalStateException(
                    "GTFS feed URL is missing: "
                            + feed.getFeedId()
            );
        }

        long start = System.currentTimeMillis();

        System.out.println("================================");
        System.out.println(
                "GTFS INGESTION STARTED: "
                        + feed.getFeedId()
        );
        System.out.println(
                "GTFS PROVIDER: "
                        + feed.getProviderName()
        );
        System.out.println(
                "GTFS LOCATION: "
                        + feed.getCountry()
                        + " / "
                        + feed.getRegion()
                        + " / "
                        + feed.getCity()
        );

        GtfsFeedData data = feedLoader.load(feed);

        if (data == null) {
            throw new IllegalStateException(
                    "GTFS loader returned no data for feed: "
                            + feed.getFeedId()
            );
        }

        System.out.println("GTFS PARSING COMPLETE");
        System.out.println(
                "STOPS: " + data.stops().size()
        );
        System.out.println(
                "ROUTES: " + data.routes().size()
        );
        System.out.println(
                "TRIPS: " + data.trips().size()
        );
        System.out.println(
                "CALENDARS: " + data.calendars().size()
        );
        System.out.println(
                "CALENDAR DATES: "
                        + data.calendarDates().size()
        );
        System.out.println(
                "STOP TIMES: "
                        + data.stopTimes().size()
        );

        repository.saveFeed(feed);
        System.out.println("GTFS FEED SAVED");

        repository.saveStops(
                feed.getFeedId(),
                data.stops()
        );
        System.out.println("GTFS STOPS SAVED");

        repository.saveRoutes(
                feed.getFeedId(),
                data.routes()
        );
        System.out.println("GTFS ROUTES SAVED");

        repository.saveTrips(
                feed.getFeedId(),
                data.trips()
        );
        System.out.println("GTFS TRIPS SAVED");

        repository.saveCalendars(
                feed.getFeedId(),
                data.calendars()
        );
        System.out.println(
                "GTFS CALENDARS SAVED: "
                        + data.calendars().size()
        );

        repository.saveCalendarDates(
                feed.getFeedId(),
                data.calendarDates()
        );
        System.out.println(
                "GTFS CALENDAR DATES SAVED: "
                        + data.calendarDates().size()
        );

        repository.saveStopTimes(
                feed.getFeedId(),
                data.stopTimes()
        );
        System.out.println("GTFS STOP TIMES SAVED");

        long duration =
                System.currentTimeMillis() - start;

        System.out.println(
                "GTFS INGESTION COMPLETED IN "
                        + duration
                        + " ms"
        );
        System.out.println("================================");
    }
}
