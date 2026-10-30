package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsFeedCatalog(
        long totalFeeds,
        long activeFeeds,
        long realtimeFeeds,
        List<GtfsCountryCatalog> countries
) {
}
