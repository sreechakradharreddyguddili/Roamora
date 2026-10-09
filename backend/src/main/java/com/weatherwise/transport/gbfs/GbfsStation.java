package com.weatherwise.transport.gbfs;

public record GbfsStation(
        String stationId,
        String name,

        Double latitude,
        Double longitude,

        Integer capacity,

        Integer bikesAvailable,
        Integer scootersAvailable,

        Boolean isRenting,
        Boolean isReturning,

        String network
) {}