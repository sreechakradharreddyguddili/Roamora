package com.weatherwise.transport.gtfs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record GtfsJourney(
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
        Duration journeyDuration,
        Integer transfers,
        String destination,
        String status,
        List<GtfsJourneyLeg> legs,
        Duration waitingTime,
        Duration transferTime,
        OffsetDateTime estimatedDepartureTime,
        OffsetDateTime estimatedArrivalTime,
        Integer delaySeconds,
        boolean realtimeAvailable,
        List<GtfsRealtimeVehicle> vehicles,
        List<GtfsRealtimeAlert> alerts
) {

    public GtfsJourney(
            String feedId, String originStopId, String originStopName,
            String destinationStopId, String destinationStopName,
            String tripId, String routeId, String routeShortName,
            String routeLongName, String routeType, String serviceId,
            LocalDateTime departureTime, LocalDateTime arrivalTime,
            Duration journeyDuration, Integer transfers,
            String destination, String status
    ) {
        this(feedId, originStopId, originStopName, destinationStopId, destinationStopName,
                tripId, routeId, routeShortName, routeLongName, routeType, serviceId,
                departureTime, arrivalTime, journeyDuration, transfers, destination, status,
                List.of(), Duration.ZERO, Duration.ZERO, null, null, null, false,
                List.of(), List.of());
    }

    public GtfsJourney(
            String feedId, String originStopId, String originStopName,
            String destinationStopId, String destinationStopName,
            String tripId, String routeId, String routeShortName,
            String routeLongName, String routeType, String serviceId,
            LocalDateTime departureTime, LocalDateTime arrivalTime,
            Duration journeyDuration, Integer transfers,
            String destination, String status, List<GtfsJourneyLeg> legs
    ) {
        this(feedId, originStopId, originStopName, destinationStopId, destinationStopName,
                tripId, routeId, routeShortName, routeLongName, routeType, serviceId,
                departureTime, arrivalTime, journeyDuration, transfers, destination, status,
                legs, Duration.ZERO, Duration.ZERO, null, null, null, false,
                List.of(), List.of());
    }

    public GtfsJourney(
            String feedId, String originStopId, String originStopName,
            String destinationStopId, String destinationStopName,
            String tripId, String routeId, String routeShortName,
            String routeLongName, String routeType, String serviceId,
            LocalDateTime departureTime, LocalDateTime arrivalTime,
            Duration journeyDuration, Integer transfers,
            String destination, String status, List<GtfsJourneyLeg> legs,
            Duration waitingTime, Duration transferTime
    ) {
        this(feedId, originStopId, originStopName, destinationStopId, destinationStopName,
                tripId, routeId, routeShortName, routeLongName, routeType, serviceId,
                departureTime, arrivalTime, journeyDuration, transfers, destination, status,
                legs, waitingTime, transferTime, null, null, null, false,
                List.of(), List.of());
    }

    public GtfsJourney(
            String feedId, String originStopId, String originStopName,
            String destinationStopId, String destinationStopName,
            String tripId, String routeId, String routeShortName,
            String routeLongName, String routeType, String serviceId,
            LocalDateTime departureTime, LocalDateTime arrivalTime,
            Duration journeyDuration, Integer transfers,
            String destination, String status, List<GtfsJourneyLeg> legs,
            Duration waitingTime, Duration transferTime,
            OffsetDateTime estimatedDepartureTime,
            OffsetDateTime estimatedArrivalTime,
            Integer delaySeconds, boolean realtimeAvailable
    ) {
        this(feedId, originStopId, originStopName, destinationStopId, destinationStopName,
                tripId, routeId, routeShortName, routeLongName, routeType, serviceId,
                departureTime, arrivalTime, journeyDuration, transfers, destination, status,
                legs, waitingTime, transferTime, estimatedDepartureTime,
                estimatedArrivalTime, delaySeconds, realtimeAvailable,
                List.of(), List.of());
    }

    public GtfsJourney(
            String feedId, String originStopId, String originStopName,
            String destinationStopId, String destinationStopName,
            String tripId, String routeId, String routeShortName,
            String routeLongName, String routeType, String serviceId,
            LocalDateTime departureTime, LocalDateTime arrivalTime,
            Duration journeyDuration, Integer transfers,
            String destination, String status, List<GtfsJourneyLeg> legs,
            Duration waitingTime, Duration transferTime,
            OffsetDateTime estimatedDepartureTime,
            OffsetDateTime estimatedArrivalTime,
            Integer delaySeconds, boolean realtimeAvailable,
            List<GtfsRealtimeVehicle> vehicles,
            List<GtfsRealtimeAlert> alerts
    ) {
        this.feedId = feedId;
        this.originStopId = originStopId;
        this.originStopName = originStopName;
        this.destinationStopId = destinationStopId;
        this.destinationStopName = destinationStopName;
        this.tripId = tripId;
        this.routeId = routeId;
        this.routeShortName = routeShortName;
        this.routeLongName = routeLongName;
        this.routeType = routeType;
        this.serviceId = serviceId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.journeyDuration = journeyDuration;
        this.transfers = transfers;
        this.destination = destination;
        this.status = status;
        this.legs = legs == null ? List.of() : List.copyOf(legs);
        this.waitingTime = waitingTime;
        this.transferTime = transferTime;
        this.estimatedDepartureTime = estimatedDepartureTime;
        this.estimatedArrivalTime = estimatedArrivalTime;
        this.delaySeconds = delaySeconds;
        this.realtimeAvailable = realtimeAvailable;
        this.vehicles = vehicles == null ? List.of() : List.copyOf(vehicles);
        this.alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public long durationMinutes() {
        return journeyDuration == null ? 0 : journeyDuration.toMinutes();
    }

    public int transferCount() {
        return transfers == null ? 0 : transfers;
    }

    public List<GtfsJourneyLeg> getLegs() {
        return legs == null ? List.of() : legs;
    }

    public List<GtfsRealtimeVehicle> getVehicles() {
        return vehicles == null ? List.of() : vehicles;
    }

    public List<GtfsRealtimeAlert> getAlerts() {
        return alerts == null ? List.of() : alerts;
    }

    public long waitingMinutes() {
        return waitingTime == null ? 0 : waitingTime.toMinutes();
    }

    public long transferMinutes() {
        return transferTime == null ? 0 : transferTime.toMinutes();
    }

    public long delayMinutes() {
        return delaySeconds == null ? 0 : Math.round(delaySeconds / 60.0);
    }

    public boolean isDelayed() {
        return delaySeconds != null && delaySeconds > 0;
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status)
                || "CANCELED".equalsIgnoreCase(status);
    }

    public boolean hasRealtime() {
        return realtimeAvailable;
    }

    public boolean hasVehicles() {
        return !getVehicles().isEmpty();
    }

    public boolean hasAlerts() {
        return !getAlerts().isEmpty();
    }
}
