package com.weatherwise.dto;

import com.weatherwise.enums.RiskLevel;
import com.weatherwise.enums.TransportMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class RouteResponse {

    private Long id;
    private String message;
    private String origin;
    private String destination;
    private TransportMode transportMode;
    private RiskLevel status;
    private Double distanceKm;
    private Integer durationMinutes;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private Integer riskScore;
    private String recommendation;
    private boolean saved;
    private LocalDateTime createdAt;

    public RouteResponse() {
    }

    public RouteResponse(
            Long id,
            String message,
            String origin,
            String destination,
            TransportMode transportMode,
            RiskLevel status,
            Double distanceKm,
            Integer durationMinutes,
            Integer riskScore,
            String recommendation,
            boolean saved,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.message = message;
        this.origin = origin;
        this.destination = destination;
        this.transportMode = transportMode;
        this.status = status;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.saved = saved;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    public RiskLevel getStatus() {
        return status;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public boolean isSaved() {
        return saved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setTransportMode(TransportMode transportMode) {
        this.transportMode = transportMode;
    }

    public void setStatus(RiskLevel status) {
        this.status = status;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}