package com.weatherwise.transport.gtfs;

import java.time.OffsetDateTime;

public record GtfsRealtimeAlert(
        Long id,
        String feedId,
        String alertId,
        String cause,
        String effect,
        String headerText,
        String descriptionText,
        String url,
        OffsetDateTime activeFrom,
        OffsetDateTime activeUntil,
        OffsetDateTime fetchedAt
) {
}
