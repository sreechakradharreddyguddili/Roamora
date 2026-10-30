package com.weatherwise.transport.gtfs;

public record GtfsDeparture(

        String stopId,
        String stopName,

        String tripId,

        String routeId,
        String routeShortName,
        String routeLongName,

        String destination,

        String serviceId,

        String scheduledArrival,
        String scheduledDeparture,

        String estimatedArrival,
        String estimatedDeparture,

        Integer delaySeconds,

        long minutesUntilDeparture,

        String status,

        boolean realtimeAvailable
) {

    public GtfsDeparture(
            String stopId,
            String stopName,
            String tripId,
            String routeId,
            String routeShortName,
            String routeLongName,
            String destination,
            String serviceId,
            String scheduledArrival,
            String scheduledDeparture,
            long minutesUntilDeparture,
            String status
    ) {
        this(
                stopId,
                stopName,
                tripId,
                routeId,
                routeShortName,
                routeLongName,
                destination,
                serviceId,
                scheduledArrival,
                scheduledDeparture,
                null,
                null,
                null,
                minutesUntilDeparture,
                status,
                false
        );
    }

    public long delayMinutes() {
        if (delaySeconds == null) {
            return 0;
        }

        return Math.max(
                0,
                Math.round(delaySeconds / 60.0)
        );
    }

    public boolean isDelayed() {
        return delaySeconds != null
                && delaySeconds > 0;
    }

    public boolean isCancelled() {
        return status != null
                && (
                status.equalsIgnoreCase("CANCELLED")
                        || status.equalsIgnoreCase("CANCELED")
        );
    }

    public boolean hasRealtime() {
        return realtimeAvailable;
    }
}