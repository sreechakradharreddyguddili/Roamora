package com.weatherwise.transport.gtfs;

public record GtfsFeedRequest(
        String feedId,
        String providerName,
        String country,
        String countryCode,
        String region,
        String city,
        String feedUrl,
        String realtimeTripUpdatesUrl,
        String realtimeVehiclePositionsUrl,
        String realtimeAlertsUrl,
        String licenseUrl,
        Boolean active,
        Boolean realtimeEnabled
) {
}
