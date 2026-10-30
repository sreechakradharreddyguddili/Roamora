package com.weatherwise.transport.gtfs;

import java.time.Duration;
import java.time.LocalDateTime;

public record GtfsTransitConnection(

        String feedId,

        String originStopId,
        String originStopName,
        String originParentStationId,

        String destinationStopId,
        String destinationStopName,
        String destinationParentStationId,

        String tripId,

        String routeId,
        String routeShortName,
        String routeLongName,
        String routeType,

        String serviceId,

        LocalDateTime departureTime,
        LocalDateTime arrivalTime,

        Duration travelDuration,

        String tripHeadsign,

        Integer fromStopSequence,
        Integer toStopSequence
) {

    public long durationMinutes() {
        if (travelDuration == null) {
            return 0;
        }

        return Math.max(
                0,
                travelDuration.toMinutes()
        );
    }

    public int fromSequence() {
        return fromStopSequence == null ? 0 : fromStopSequence;
    }

    public int toSequence() {
        return toStopSequence == null ? 0 : toStopSequence;
    }

    public boolean hasOriginParentStation() {
        return originParentStationId != null
                && !originParentStationId.isBlank();
    }

    public boolean hasDestinationParentStation() {
        return destinationParentStationId != null
                && !destinationParentStationId.isBlank();
    }
}
