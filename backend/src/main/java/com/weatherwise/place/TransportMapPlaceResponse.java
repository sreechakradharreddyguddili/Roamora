package com.weatherwise.place;

public record TransportMapPlaceResponse(
        String id,
        String name,
        String type,
        Double latitude,
        Double longitude,
        String city,
        String country,
        String operatorName,
        String code,
        String source,
        Double distanceKm
) {}
