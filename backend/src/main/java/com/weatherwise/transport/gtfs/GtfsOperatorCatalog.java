package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsOperatorCatalog(
        String providerName,
        long feedCount,
        List<GtfsFeed> feeds
) {
}
