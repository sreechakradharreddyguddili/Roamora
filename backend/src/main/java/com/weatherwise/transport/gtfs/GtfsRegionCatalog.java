package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsRegionCatalog(
        String region,
        long feedCount,
        List<GtfsCityCatalog> cities
) {
}
