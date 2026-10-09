package com.weatherwise.transport.catalog;

public record MobilityDatabaseFeed(
        String id,
        String provider,
        String name,

        String countryCode,
        String subdivisionName,
        String municipality,

        String status,

        boolean official,

        String downloadUrl,
        String latestUrl,
        String licenseUrl,

        Integer authenticationType,
        String authenticationInfoUrl,
        String apiKeyParameterName
) {
}