package com.weatherwise.transport.gtfs;

public record GtfsStop(
        String stopId,
        String stopCode,
        String stopName,
        Double latitude,
        Double longitude,
        String locationType,
        String parentStation,
        String timezone,
        String platformCode,
        String levelId,
        String wheelchairBoarding,
        String zoneId
) {

    public GtfsStop(
            String stopId,
            String stopCode,
            String stopName,
            Double latitude,
            Double longitude,
            String locationType,
            String parentStation,
            String timezone
    ) {
        this(
                stopId,
                stopCode,
                stopName,
                latitude,
                longitude,
                locationType,
                parentStation,
                timezone,
                null,
                null,
                null,
                null
        );
    }
}