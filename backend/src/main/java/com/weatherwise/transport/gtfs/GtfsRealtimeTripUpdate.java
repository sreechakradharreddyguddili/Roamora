package com.weatherwise.transport.gtfs;

import java.time.OffsetDateTime;

public record GtfsRealtimeTripUpdate(
        Long id,
        String feedId,
        String tripId,
        String routeId,
        String stopId,
        Integer stopSequence,
        OffsetDateTime scheduledTime,
        OffsetDateTime estimatedTime,
        Integer delaySeconds,
        String status,
        OffsetDateTime fetchedAt
) {
}
