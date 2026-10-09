package com.weatherwise.transport.gtfs;

public record GtfsTransportPlaceResult(

        String stopId,

        String stopName,

        String stopCode,

        Double latitude,

        Double longitude,

        String locationType,

        String parentStation,

        String timezone,

        String city,

        String country,

        String countryCode,

        String region

) {

}