package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsCountryCatalog(
        String country,
        String countryCode,
        long feedCount,
        long activeFeedCount,
        List<GtfsRegionCatalog> regions
) {
}
