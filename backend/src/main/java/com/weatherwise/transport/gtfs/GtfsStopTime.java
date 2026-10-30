package com.weatherwise.transport.gtfs;

public record GtfsStopTime(
        String tripId,
        String arrivalTime,
        String departureTime,
        String stopId,
        Integer stopSequence,
        String stopHeadsign,
        String pickupType,
        String dropOffType,
        String shapeDistTraveled
) {

    public GtfsStopTime(
            String tripId,
            String arrivalTime,
            String departureTime,
            String stopId,
            Integer stopSequence,
            String stopHeadsign
    ) {
        this(
                tripId,
                arrivalTime,
                departureTime,
                stopId,
                stopSequence,
                stopHeadsign,
                null,
                null,
                null
        );
    }
}