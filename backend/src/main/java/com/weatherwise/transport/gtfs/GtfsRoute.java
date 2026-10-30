package com.weatherwise.transport.gtfs;

public record GtfsRoute(
        String routeId,
        String routeShortName,
        String routeLongName,
        String routeType,
        String agencyId,
        String routeColor,
        String routeTextColor,
        String routeUrl
) {

    public GtfsRoute(
            String routeId,
            String routeShortName,
            String routeLongName,
            String routeType,
            String agencyId
    ) {
        this(
                routeId,
                routeShortName,
                routeLongName,
                routeType,
                agencyId,
                null,
                null,
                null
        );
    }
}