package com.weatherwise.place;

public record PlaceResponse(
        String id,
        String name,
        PlaceCategory category,
        Double latitude,
        Double longitude,
        String address,
        String website,
        String phone,
        Double distanceKm,
        Double starRating,
        String source
) {}
