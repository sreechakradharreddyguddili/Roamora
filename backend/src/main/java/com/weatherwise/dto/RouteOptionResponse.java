package com.weatherwise.dto;

import com.weatherwise.enums.RiskLevel;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.transport.flight.FlightLeg;
import com.weatherwise.transport.global.MultimodalTransitOption;
import com.weatherwise.transport.gtfs.GtfsJourney;
import com.weatherwise.weather.RouteWeatherPoint;

import java.time.OffsetDateTime;
import java.util.List;

public class RouteOptionResponse {

    private String routeKey;

    private Double distanceKm;

    private Integer durationMinutes;

    private Integer riskScore;

    private RiskLevel riskLevel;

    private String recommendation;

    private Boolean recommended;

    private String geometry;

    private List<RouteWeatherPoint> weatherAlongRoute;

    private Double combinedScore;

    /*
     * Generic multimodal journey details.
     *
     * This preserves the first-mile connector, the real GTFS journey,
     * and the last-mile connector without abusing the flight-specific
     * fields above.
     */
    private MultimodalTransitOption multimodalOption;

    private String multimodalStatus;

    private String multimodalMessage;

    /*
     * Scheduled/public-transport journey.
     *
     * This is populated for transport modes such as:
     * BUS, COACH, METRO, SUBWAY, TRAM, LIGHT_RAIL,
     * MONORAIL, TRAIN, COMMUTER_RAIL, HIGH_SPEED_TRAIN,
     * FERRY and SHIP where GTFS journey data is available.
     */
    private GtfsJourney journey;

    /*
     * ============================================================
     * FLIGHT CONNECTION
     * ============================================================
     *
     * Populated when the route is planned through:
     *
     * origin
     *   -> origin airport
     *   -> flight
     *   -> destination airport
     *   -> onward transit place
     *   -> GTFS journey
     *   -> destination
     *
     * These fields preserve the complete multimodal flight
     * connection instead of reducing it to a generic route.
     */

    private TransportPlace originAirport;

    private TransportPlace destinationAirport;

    private FlightLeg flight;

    /*
     * Transit place where the onward GTFS journey begins.
     */
    private TransportPlace onwardBoardingPlace;

    /*
     * Transit place where the onward GTFS journey ends.
     */
    private TransportPlace onwardAlightingPlace;

    /*
     * Individual timing components of the flight connection.
     */
    private Integer originAccessMinutes;

    private Integer airportDepartureBufferMinutes;

    private Integer airportToTransitMinutes;

    private Integer flightDurationMinutes;

    private Integer flightToTransitWaitingMinutes;

    private Integer transitDurationMinutes;

    private Integer finalJourneyMinutes;

    private Integer totalJourneyMinutes;

    /*
     * Planned flight departure and effective flight arrival.
     *
     * Effective arrival may use estimated arrival when
     * realtime/live flight data is available.
     */
    private OffsetDateTime flightDeparture;

    private OffsetDateTime flightArrival;

    /*
     * Availability information specifically for the
     * flight connection.
     */
    private String flightConnectionStatus;

    private String flightConnectionMessage;

    public RouteOptionResponse() {
    }

    public RouteOptionResponse(
            String routeKey,
            Double distanceKm,
            Integer durationMinutes,
            Integer riskScore,
            RiskLevel riskLevel,
            String recommendation,
            Boolean recommended,
            String geometry,
            List<RouteWeatherPoint> weatherAlongRoute
    ) {
        this.routeKey = routeKey;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
        this.recommended = recommended;
        this.geometry = geometry;
        this.weatherAlongRoute = weatherAlongRoute;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public Boolean getRecommended() {
        return recommended;
    }

    public String getGeometry() {
        return geometry;
    }

    public List<RouteWeatherPoint> getWeatherAlongRoute() {
        return weatherAlongRoute;
    }

    public Double getCombinedScore() {
        return combinedScore;
    }

    public MultimodalTransitOption getMultimodalOption() {
        return multimodalOption;
    }

    public String getMultimodalStatus() {
        return multimodalStatus;
    }

    public String getMultimodalMessage() {
        return multimodalMessage;
    }

    public GtfsJourney getJourney() {
        return journey;
    }

    public TransportPlace getOriginAirport() {
        return originAirport;
    }

    public TransportPlace getDestinationAirport() {
        return destinationAirport;
    }

    public FlightLeg getFlight() {
        return flight;
    }

    public TransportPlace getOnwardBoardingPlace() {
        return onwardBoardingPlace;
    }

    public TransportPlace getOnwardAlightingPlace() {
        return onwardAlightingPlace;
    }

    public Integer getOriginAccessMinutes() {
        return originAccessMinutes;
    }

    public Integer getAirportDepartureBufferMinutes() {
        return airportDepartureBufferMinutes;
    }

    public Integer getAirportToTransitMinutes() {
        return airportToTransitMinutes;
    }

    public Integer getFlightDurationMinutes() {
        return flightDurationMinutes;
    }

    public Integer getFlightToTransitWaitingMinutes() {
        return flightToTransitWaitingMinutes;
    }

    public Integer getTransitDurationMinutes() {
        return transitDurationMinutes;
    }

    public Integer getFinalJourneyMinutes() {
        return finalJourneyMinutes;
    }

    public Integer getTotalJourneyMinutes() {
        return totalJourneyMinutes;
    }

    public OffsetDateTime getFlightDeparture() {
        return flightDeparture;
    }

    public OffsetDateTime getFlightArrival() {
        return flightArrival;
    }

    public String getFlightConnectionStatus() {
        return flightConnectionStatus;
    }

    public String getFlightConnectionMessage() {
        return flightConnectionMessage;
    }

    public void setRouteKey(
            String routeKey
    ) {
        this.routeKey = routeKey;
    }

    public void setDistanceKm(
            Double distanceKm
    ) {
        this.distanceKm = distanceKm;
    }

    public void setDurationMinutes(
            Integer durationMinutes
    ) {
        this.durationMinutes = durationMinutes;
    }

    public void setRiskScore(
            Integer riskScore
    ) {
        this.riskScore = riskScore;
    }

    public void setRiskLevel(
            RiskLevel riskLevel
    ) {
        this.riskLevel = riskLevel;
    }

    public void setRecommendation(
            String recommendation
    ) {
        this.recommendation = recommendation;
    }

    public void setRecommended(
            Boolean recommended
    ) {
        this.recommended = recommended;
    }

    public void setGeometry(
            String geometry
    ) {
        this.geometry = geometry;
    }

    public void setWeatherAlongRoute(
            List<RouteWeatherPoint> weatherAlongRoute
    ) {
        this.weatherAlongRoute = weatherAlongRoute;
    }

    public void setCombinedScore(
            Double combinedScore
    ) {
        this.combinedScore = combinedScore;
    }

    public void setMultimodalOption(
            MultimodalTransitOption multimodalOption
    ) {
        this.multimodalOption = multimodalOption;
    }

    public void setMultimodalStatus(
            String multimodalStatus
    ) {
        this.multimodalStatus = multimodalStatus;
    }

    public void setMultimodalMessage(
            String multimodalMessage
    ) {
        this.multimodalMessage = multimodalMessage;
    }

    public void setJourney(
            GtfsJourney journey
    ) {
        this.journey = journey;
    }

    public void setOriginAirport(
            TransportPlace originAirport
    ) {
        this.originAirport = originAirport;
    }

    public void setDestinationAirport(
            TransportPlace destinationAirport
    ) {
        this.destinationAirport = destinationAirport;
    }

    public void setFlight(
            FlightLeg flight
    ) {
        this.flight = flight;
    }

    public void setOnwardBoardingPlace(
            TransportPlace onwardBoardingPlace
    ) {
        this.onwardBoardingPlace = onwardBoardingPlace;
    }

    public void setOnwardAlightingPlace(
            TransportPlace onwardAlightingPlace
    ) {
        this.onwardAlightingPlace = onwardAlightingPlace;
    }

    public void setOriginAccessMinutes(
            Integer originAccessMinutes
    ) {
        this.originAccessMinutes = originAccessMinutes;
    }

    public void setAirportDepartureBufferMinutes(
            Integer airportDepartureBufferMinutes
    ) {
        this.airportDepartureBufferMinutes =
                airportDepartureBufferMinutes;
    }

    public void setAirportToTransitMinutes(
            Integer airportToTransitMinutes
    ) {
        this.airportToTransitMinutes =
                airportToTransitMinutes;
    }

    public void setFlightDurationMinutes(
            Integer flightDurationMinutes
    ) {
        this.flightDurationMinutes =
                flightDurationMinutes;
    }

    public void setFlightToTransitWaitingMinutes(
            Integer flightToTransitWaitingMinutes
    ) {
        this.flightToTransitWaitingMinutes =
                flightToTransitWaitingMinutes;
    }

    public void setTransitDurationMinutes(
            Integer transitDurationMinutes
    ) {
        this.transitDurationMinutes =
                transitDurationMinutes;
    }

    public void setFinalJourneyMinutes(
            Integer finalJourneyMinutes
    ) {
        this.finalJourneyMinutes =
                finalJourneyMinutes;
    }

    public void setTotalJourneyMinutes(
            Integer totalJourneyMinutes
    ) {
        this.totalJourneyMinutes =
                totalJourneyMinutes;
    }

    public void setFlightDeparture(
            OffsetDateTime flightDeparture
    ) {
        this.flightDeparture = flightDeparture;
    }

    public void setFlightArrival(
            OffsetDateTime flightArrival
    ) {
        this.flightArrival = flightArrival;
    }

    public void setFlightConnectionStatus(
            String flightConnectionStatus
    ) {
        this.flightConnectionStatus =
                flightConnectionStatus;
    }

    public void setFlightConnectionMessage(
            String flightConnectionMessage
    ) {
        this.flightConnectionMessage =
                flightConnectionMessage;
    }
}