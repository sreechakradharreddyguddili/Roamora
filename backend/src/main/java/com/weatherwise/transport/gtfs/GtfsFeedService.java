package com.weatherwise.transport.gtfs;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GtfsFeedService {

    private final GtfsFeedRepository feedRepository;
    private final GtfsRepository gtfsRepository;
    private final GtfsRealtimeService realtimeService;
    private final GtfsIngestionService ingestionService;

    public GtfsFeedService(
            GtfsFeedRepository feedRepository,
            GtfsRepository gtfsRepository,
            GtfsRealtimeService realtimeService,
            GtfsIngestionService ingestionService
    ) {
        this.feedRepository = feedRepository;
        this.gtfsRepository = gtfsRepository;
        this.realtimeService = realtimeService;
        this.ingestionService = ingestionService;
    }

    public List<GtfsFeed> findFeeds(
            String country,
            String region,
            String city,
            Boolean active,
            Boolean realtimeEnabled
    ) {
        return feedRepository.findAll(
                country,
                region,
                city,
                active,
                realtimeEnabled
        );
    }


    public List<GtfsFeed> findActiveStaticFeeds() {
        return feedRepository.findActiveStaticFeeds();
    }

    public List<GtfsFeed> findActiveRealtimeFeeds() {
        return feedRepository.findActiveRealtimeFeeds();
    }

    public GtfsFeed findFeed(String feedId) {
        return requireFeed(feedId);
    }

    public GtfsFeed create(GtfsFeedRequest request) {
        validateRequest(request);

        String feedId = required(
                request.feedId(),
                "feedId is required"
        );

        if (feedRepository.exists(feedId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "GTFS feed already exists: " + feedId
            );
        }

        GtfsFeed feed = toFeed(request, feedId, true);
        gtfsRepository.saveFeed(feed);

        return requireFeed(feedId);
    }

    public GtfsFeed update(
            String feedId,
            GtfsFeedRequest request
    ) {
        String normalizedFeedId = required(
                feedId,
                "feedId is required"
        );

        requireFeed(normalizedFeedId);
        validateRequest(request);

        GtfsFeed feed = toFeed(
                request,
                normalizedFeedId,
                false
        );

        gtfsRepository.saveFeed(feed);

        return requireFeed(normalizedFeedId);
    }

    public GtfsFeed setActive(
            String feedId,
            boolean active
    ) {
        GtfsFeed feed = requireFeed(feedId);
        feed.setActive(active);

        gtfsRepository.saveFeed(feed);

        return requireFeed(feed.getFeedId());
    }

    public GtfsFeed setRealtimeEnabled(
            String feedId,
            boolean realtimeEnabled
    ) {
        GtfsFeed feed = requireFeed(feedId);
        feed.setRealtimeEnabled(realtimeEnabled);

        gtfsRepository.saveFeed(feed);

        return requireFeed(feed.getFeedId());
    }

    public GtfsRealtimeRefreshResult refreshRealtime(
            String feedId
    ) {
        GtfsFeed feed = requireFeed(feedId);

        if (!feed.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "GTFS feed is inactive: "
                            + feed.getFeedId()
            );
        }

        if (!feed.isRealtimeEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Realtime is disabled for feed: "
                            + feed.getFeedId()
            );
        }

        return realtimeService.refreshFeed(feed);
    }

    public void ingestFeed(String feedId) {
        GtfsFeed feed = requireFeed(feedId);

        if (!feed.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "GTFS feed is inactive: "
                            + feed.getFeedId()
            );
        }

        ingestionService.ingest(feed);
    }

    public GtfsFeed deactivate(String feedId) {
        return setActive(feedId, false);
    }

    private GtfsFeed requireFeed(String feedId) {
        String normalized = required(
                feedId,
                "feedId is required"
        );

        GtfsFeed feed =
                feedRepository.findById(normalized);

        if (feed == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "GTFS feed not found: "
                            + normalized
            );
        }

        return feed;
    }

    private void validateRequest(
            GtfsFeedRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body is required"
            );
        }

        required(
                request.providerName(),
                "providerName is required"
        );

        required(
                request.country(),
                "country is required"
        );

        required(
                request.countryCode(),
                "countryCode is required"
        );

        required(
                request.feedUrl(),
                "feedUrl is required"
        );
    }

    private GtfsFeed toFeed(
            GtfsFeedRequest request,
            String feedId,
            boolean creating
    ) {
        GtfsFeed feed = new GtfsFeed();

        feed.setFeedId(feedId);
        feed.setProviderName(
                trim(request.providerName())
        );
        feed.setCountry(
                trim(request.country())
        );
        feed.setCountryCode(
                trim(request.countryCode())
        );
        feed.setRegion(
                trim(request.region())
        );
        feed.setCity(
                trim(request.city())
        );
        feed.setFeedUrl(
                trim(request.feedUrl())
        );

        feed.setRealtimeTripUpdatesUrl(
                trim(request.realtimeTripUpdatesUrl())
        );

        feed.setRealtimeVehiclePositionsUrl(
                trim(request.realtimeVehiclePositionsUrl())
        );

        feed.setRealtimeAlertsUrl(
                trim(request.realtimeAlertsUrl())
        );

        feed.setLicenseUrl(
                trim(request.licenseUrl())
        );

        if (creating) {
            feed.setActive(
                    request.active() == null
                            || request.active()
            );

            feed.setRealtimeEnabled(
                    request.realtimeEnabled() != null
                            && request.realtimeEnabled()
            );
        } else {
            GtfsFeed current =
                    requireFeed(feedId);

            feed.setActive(
                    request.active() == null
                            ? current.isActive()
                            : request.active()
            );

            feed.setRealtimeEnabled(
                    request.realtimeEnabled() == null
                            ? current.isRealtimeEnabled()
                            : request.realtimeEnabled()
            );
        }

        return feed;
    }

    private String required(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }

        return value.trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
