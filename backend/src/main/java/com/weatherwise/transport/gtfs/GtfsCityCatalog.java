package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsCityCatalog(
        String city,
        long feedCount,
        List<GtfsOperatorCatalog> operators
) {
}
