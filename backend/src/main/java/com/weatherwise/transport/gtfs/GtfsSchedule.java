package com.weatherwise.transport.gtfs;

public record GtfsSchedule(
        String stopId,
        String stopName,
        String stopTimezone,

        String tripId,
        String arrivalTime,
        String departureTime,
        Integer stopSequence,
        String stopHeadsign,

        String routeId,
        String serviceId,
        String tripHeadsign,
        String directionId,

        String routeShortName,
        String routeLongName,
        String routeType
) {}