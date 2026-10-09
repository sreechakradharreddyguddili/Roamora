package com.weatherwise.dto;

import com.weatherwise.enums.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class RouteRequest {

    @NotBlank(message = "Origin is required")
    @Size(
            min = 2,
            max = 100,
            message = "Origin must be between 2 and 100 characters"
    )
    private String origin;

    @NotBlank(message = "Destination is required")
    @Size(
            min = 2,
            max = 100,
            message = "Destination must be between 2 and 100 characters"
    )
    private String destination;

    @NotNull(message = "Transport mode is required")
    private TransportMode transportMode;

    private LocalDate departureDate;

    private LocalTime departureTime;

    /*
     * Coordinates are optional.
     *
     * If supplied, RouteService uses them directly.
     * If not supplied, RouteService falls back to geocoding
     * the origin/destination text.
     */
    private Double originLatitude;

    private Double originLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    public RouteRequest() {
    }

    public RouteRequest(
            String origin,
            String destination,
            TransportMode transportMode
    ) {
        this(
                origin,
                destination,
                transportMode,
                null,
                null
        );
    }

    public RouteRequest(
            String origin,
            String destination,
            TransportMode transportMode,
            LocalDate departureDate
    ) {
        this(
                origin,
                destination,
                transportMode,
                departureDate,
                null
        );
    }

    public RouteRequest(
            String origin,
            String destination,
            TransportMode transportMode,
            LocalDate departureDate,
            LocalTime departureTime
    ) {
        this.origin = origin;
        this.destination = destination;
        this.transportMode = transportMode;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(
            TransportMode transportMode
    ) {
        this.transportMode = transportMode;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(
            LocalDate departureDate
    ) {
        this.departureDate = departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(
            LocalTime departureTime
    ) {
        this.departureTime = departureTime;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public void setOriginLatitude(
            Double originLatitude
    ) {
        this.originLatitude = originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public void setOriginLongitude(
            Double originLongitude
    ) {
        this.originLongitude = originLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(
            Double destinationLatitude
    ) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(
            Double destinationLongitude
    ) {
        this.destinationLongitude = destinationLongitude;
    }
}