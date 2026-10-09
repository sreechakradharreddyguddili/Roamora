package com.weatherwise.transport.gtfs;

public record GtfsTrip(

        String tripId,

        String routeId,

        String serviceId,

        String tripHeadsign,

        String directionId

) {
}