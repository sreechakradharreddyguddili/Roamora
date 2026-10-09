package com.weatherwise.dto;

import com.weatherwise.enums.RiskLevel;
import com.weatherwise.enums.TransportMode;
import com.weatherwise.transport.gtfs.GtfsJourney;
import com.weatherwise.weather.RouteWeatherPoint;

import java.time.LocalDateTime;
import java.util.List;

public class RouteAnalysisResponse {

    private Long routeId;

    private String origin;

    private String destination;

    private TransportMode transportMode;

    private double originTemperature;

    private double destinationTemperature;

    private double originWindSpeed;

    private double destinationWindSpeed;

    private double originHumidity;

    private double destinationHumidity;

    private double originPrecipitation;

    private double destinationPrecipitation;

    private int originWeatherCode;

    private int destinationWeatherCode;

    private RiskLevel riskLevel;

    private Integer riskScore;

    private String recommendation;

    private Double distanceKm;

    private Integer durationMinutes;

    private String routeGeometry;

    private Boolean mapAvailable;

    private List<RouteWeatherPoint> weatherAlongRoute;

    private List<RouteOptionResponse> alternativeRoutes;

    /*
     * Selected scheduled/public-transport journey.
     *
     * This is populated when the selected route
     * comes from the GTFS journey layer.
     */
    private GtfsJourney journey;

    private LocalDateTime createdAt;

    public RouteAnalysisResponse() {
    }

    public RouteAnalysisResponse(
            Long routeId,
            String origin,
            String destination,
            TransportMode transportMode,
            double originTemperature,
            double destinationTemperature,
            double originWindSpeed,
            double destinationWindSpeed,
            RiskLevel riskLevel,
            Integer riskScore,
            String recommendation,
            Double distanceKm,
            Integer durationMinutes,
            String routeGeometry,
            List<RouteWeatherPoint> weatherAlongRoute,
            List<RouteOptionResponse> alternativeRoutes,
            LocalDateTime createdAt
    ) {
        this.routeId = routeId;
        this.origin = origin;
        this.destination = destination;
        this.transportMode = transportMode;
        this.originTemperature = originTemperature;
        this.destinationTemperature = destinationTemperature;
        this.originWindSpeed = originWindSpeed;
        this.destinationWindSpeed = destinationWindSpeed;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.routeGeometry = routeGeometry;
        this.weatherAlongRoute = weatherAlongRoute;
        this.alternativeRoutes = alternativeRoutes;
        this.createdAt = createdAt;
    }

    public Long getRouteId() {
        return routeId;
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

    public double getOriginTemperature() {
        return originTemperature;
    }

    public double getDestinationTemperature() {
        return destinationTemperature;
    }

    public double getOriginWindSpeed() {
        return originWindSpeed;
    }

    public double getDestinationWindSpeed() {
        return destinationWindSpeed;
    }

    public double getOriginHumidity() {
        return originHumidity;
    }

    public double getDestinationHumidity() {
        return destinationHumidity;
    }

    public double getOriginPrecipitation() {
        return originPrecipitation;
    }

    public double getDestinationPrecipitation() {
        return destinationPrecipitation;
    }

    public int getOriginWeatherCode() {
        return originWeatherCode;
    }

    public int getDestinationWeatherCode() {
        return destinationWeatherCode;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getRouteGeometry() {
        return routeGeometry;
    }

    public Boolean getMapAvailable() {
        return mapAvailable;
    }

    public List<RouteWeatherPoint> getWeatherAlongRoute() {
        return weatherAlongRoute;
    }

    public List<RouteOptionResponse> getAlternativeRoutes() {
        return alternativeRoutes;
    }

    public GtfsJourney getJourney() {
        return journey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setTransportMode(
            TransportMode transportMode
    ) {
        this.transportMode = transportMode;
    }

    public void setOriginTemperature(
            double originTemperature
    ) {
        this.originTemperature = originTemperature;
    }

    public void setDestinationTemperature(
            double destinationTemperature
    ) {
        this.destinationTemperature =
                destinationTemperature;
    }

    public void setOriginWindSpeed(
            double originWindSpeed
    ) {
        this.originWindSpeed =
                originWindSpeed;
    }

    public void setDestinationWindSpeed(
            double destinationWindSpeed
    ) {
        this.destinationWindSpeed =
                destinationWindSpeed;
    }

    public void setOriginHumidity(double originHumidity) {
        this.originHumidity = originHumidity;
    }

    public void setDestinationHumidity(double destinationHumidity) {
        this.destinationHumidity = destinationHumidity;
    }

    public void setOriginPrecipitation(double originPrecipitation) {
        this.originPrecipitation = originPrecipitation;
    }

    public void setDestinationPrecipitation(double destinationPrecipitation) {
        this.destinationPrecipitation = destinationPrecipitation;
    }

    public void setOriginWeatherCode(int originWeatherCode) {
        this.originWeatherCode = originWeatherCode;
    }

    public void setDestinationWeatherCode(int destinationWeatherCode) {
        this.destinationWeatherCode = destinationWeatherCode;
    }

    public void setRiskLevel(
            RiskLevel riskLevel
    ) {
        this.riskLevel = riskLevel;
    }

    public void setRiskScore(
            Integer riskScore
    ) {
        this.riskScore = riskScore;
    }

    public void setRecommendation(
            String recommendation
    ) {
        this.recommendation = recommendation;
    }

    public void setDistanceKm(
            Double distanceKm
    ) {
        this.distanceKm = distanceKm;
    }

    public void setDurationMinutes(
            Integer durationMinutes
    ) {
        this.durationMinutes =
                durationMinutes;
    }

    public void setRouteGeometry(
            String routeGeometry
    ) {
        this.routeGeometry =
                routeGeometry;
    }

    public void setMapAvailable(
            Boolean mapAvailable
    ) {
        this.mapAvailable = mapAvailable;
    }

    public void setWeatherAlongRoute(
            List<RouteWeatherPoint> weatherAlongRoute
    ) {
        this.weatherAlongRoute =
                weatherAlongRoute;
    }

    public void setAlternativeRoutes(
            List<RouteOptionResponse> alternativeRoutes
    ) {
        this.alternativeRoutes =
                alternativeRoutes;
    }

    public void setJourney(
            GtfsJourney journey
    ) {
        this.journey = journey;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}