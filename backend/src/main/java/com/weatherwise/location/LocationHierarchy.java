package com.weatherwise.location;

public record LocationHierarchy(
        String countryCode,
        String country,

        String regionCode,
        String region,

        String city,

        Double latitude,
        Double longitude,

        String timezone
) {}