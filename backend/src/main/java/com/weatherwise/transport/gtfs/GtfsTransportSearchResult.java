package com.weatherwise.transport.gtfs;

import com.weatherwise.enums.TransportMode;

public record GtfsTransportSearchResult(
        String feedId,
        String stopId,
        String stopName,

        String city,
        String country,
        String countryCode,

        Double latitude,
        Double longitude,

        String timezone,

        TransportMode mode,

        String routeId,
        String routeNumber,
        String routeName,

        String operatorName
) {}