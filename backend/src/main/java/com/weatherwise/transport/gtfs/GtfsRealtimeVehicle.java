package com.weatherwise.transport.gtfs;

import java.time.OffsetDateTime;

public record GtfsRealtimeVehicle(
        Long id,
        String feedId,
        String vehicleId,
        String tripId,
        String routeId,
        Double latitude,
        Double longitude,
        Double bearing,
        Double speed,
        Integer currentStopSequence,
        String currentStatus,
        OffsetDateTime fetchedAt
) {
}
