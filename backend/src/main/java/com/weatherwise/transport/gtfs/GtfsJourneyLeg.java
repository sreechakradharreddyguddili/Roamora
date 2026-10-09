package com.weatherwise.transport.gtfs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record GtfsJourneyLeg(
        String feedId,

        String originStopId,
        String originStopName,

        String destinationStopId,
        String destinationStopName,

        String tripId,

        String routeId,
        String routeShortName,
        String routeLongName,
        String routeType,

        String serviceId,

        LocalDateTime departureTime,
        LocalDateTime arrivalTime,

        Duration travelDuration,

        Integer stopCount,

        String destination,
        String status,

        OffsetDateTime estimatedDepartureTime,
        OffsetDateTime estimatedArrivalTime,

        Integer delaySeconds,
        boolean realtimeAvailable
) {

    /*
     * Backward-compatible constructor.
     *
     * Existing repositories and journey-planner code can continue
     * creating scheduled legs without realtime fields.
     */
    public GtfsJourneyLeg(
            String feedId,
            String originStopId,
            String originStopName,
            String destinationStopId,
            String destinationStopName,
            String tripId,
            String routeId,
            String routeShortName,
            String routeLongName,
            String routeType,
            String serviceId,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            Duration travelDuration,
            Integer stopCount,
            String destination,
            String status
    ) {
        this(
                feedId,
                originStopId,
                originStopName,
                destinationStopId,
                destinationStopName,
                tripId,
                routeId,
                routeShortName,
                routeLongName,
                routeType,
                serviceId,
                departureTime,
                arrivalTime,
                travelDuration,
                stopCount,
                destination,
                status,
                null,
                null,
                null,
                false
        );
    }

    public long durationMinutes() {
        return travelDuration == null
                ? 0
                : Math.max(0, travelDuration.toMinutes());
    }

    public int getStopCount() {
        return stopCount == null
                ? 0
                : Math.max(0, stopCount);
    }

    public String getStatus() {
        return status == null || status.isBlank()
                ? "SCHEDULED"
                : status;
    }

    public String getDestination() {
        return destination;
    }

    public long delayMinutes() {
        if (delaySeconds == null) {
            return 0;
        }

        return Math.round(delaySeconds / 60.0);
    }

    public boolean isDelayed() {
        return delaySeconds != null
                && delaySeconds > 0;
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(getStatus())
                || "CANCELED".equalsIgnoreCase(getStatus());
    }

    public boolean hasRealtime() {
        return realtimeAvailable;
    }
}