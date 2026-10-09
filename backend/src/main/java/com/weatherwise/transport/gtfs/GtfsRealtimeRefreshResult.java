package com.weatherwise.transport.gtfs;

import java.time.OffsetDateTime;

public record GtfsRealtimeRefreshResult(
        String feedId,
        OffsetDateTime fetchedAt,
        int tripUpdateCount,
        int vehicleCount,
        int alertCount
) {
}
