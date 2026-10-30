package com.weatherwise.service;

import com.weatherwise.enums.RiskLevel;
import com.weatherwise.enums.TransportMode;
import com.weatherwise.weather.RouteWeatherPoint;
import com.weatherwise.weather.WeatherResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteRiskService {

    public RiskLevel calculateRisk(
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            List<RouteWeatherPoint> routeWeather,
            TransportMode transportMode
    ) {

        int score =
                calculateRiskScore(
                        originWeather,
                        destinationWeather,
                        routeWeather,
                        transportMode
                );

        if (score >= 70) {
            return RiskLevel.HIGH;
        }

        if (score >= 40) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    public int calculateRiskScore(
            WeatherResponse originWeather,
            WeatherResponse destinationWeather,
            List<RouteWeatherPoint> routeWeather,
            TransportMode transportMode
    ) {

        int maximumScore = 0;

        maximumScore =
                Math.max(
                        maximumScore,
                        calculatePointScore(
                                originWeather.getWindSpeed(),
                                originWeather.getWeatherCode(),
                                transportMode
                        )
                );

        maximumScore =
                Math.max(
                        maximumScore,
                        calculatePointScore(
                                destinationWeather.getWindSpeed(),
                                destinationWeather.getWeatherCode(),
                                transportMode
                        )
                );

        if (routeWeather != null) {

            for (RouteWeatherPoint point :
                    routeWeather) {

                int pointScore =
                        calculatePointScore(
                                point.windSpeed(),
                                point.weatherCode(),
                                transportMode
                        );

                maximumScore =
                        Math.max(
                                maximumScore,
                                pointScore
                        );
            }
        }

        return Math.min(
                maximumScore,
                100
        );
    }

    private int calculatePointScore(
            double windSpeed,
            int weatherCode,
            TransportMode transportMode
    ) {

        int score = 0;

        if (isSevereWeather(weatherCode)) {

            score += 65;

        } else if (isModerateWeather(weatherCode)) {

            score += 30;
        }

        if (windSpeed >= 60) {

            score += 35;

        } else if (windSpeed >= 50) {

            score += 30;

        } else if (windSpeed >= 40) {

            score += 22;

        } else if (windSpeed >= 30) {

            score += 12;
        }

        score =
                applyTransportAdjustment(
                        score,
                        transportMode,
                        weatherCode,
                        windSpeed
                );

        return Math.min(
                score,
                100
        );
    }

    private int applyTransportAdjustment(
            int score,
            TransportMode transportMode,
            int weatherCode,
            double windSpeed
    ) {

        if (transportMode == null) {
            return score;
        }

        return switch (transportMode) {

            /*
             * Walking is particularly affected by
             * heavy rain and strong wind.
             */
            case WALKING ->
                    score +
                            walkingAdjustment(
                                    weatherCode,
                                    windSpeed
                            );

            /*
             * Bicycle and scooter have similar
             * exposure to rain and wind.
             */
            case BICYCLE,
                 SCOOTER ->

                    score +
                            bicycleAdjustment(
                                    weatherCode,
                                    windSpeed
                            );

            /*
             * Motorcycle is exposed to rain and
             * strong wind.
             */
            case MOTORCYCLE ->

                    score +
                            motorcycleAdjustment(
                                    weatherCode,
                                    windSpeed
                            );

            /*
             * Cars and taxis are road vehicles.
             * Existing road-weather score is retained.
             */
            case CAR,
                 TAXI,
                 AUTO_RICKSHAW ->

                    score;

            /*
             * Bus and coach are road-based public
             * transport.
             */
            case BUS,
                 COACH ->

                    score;

            /*
             * Metro, subway, tram, light rail,
             * commuter rail and trains generally
             * use the base weather score.
             *
             * Weather can still affect the journey,
             * but we do not invent additional penalties.
             */
            case METRO,
                 SUBWAY,
                 TRAM,
                 LIGHT_RAIL,
                 TRAIN,
                 COMMUTER_RAIL,
                 HIGH_SPEED_TRAIN,
                 MONORAIL ->

                    score;

            /*
             * Flight-specific weather adjustment.
             */
            case FLIGHT ->

                    score +
                            flightAdjustment(
                                    weatherCode,
                                    windSpeed
                            );

            /*
             * Water transport is more sensitive to
             * strong winds and storms.
             */
            case FERRY,
                 SHIP ->

                    score +
                            shipAdjustment(
                                    weatherCode,
                                    windSpeed
                            );

            /*
             * Multimodal journeys use the base weather
             * score here. Individual legs can later
             * receive their own transport-specific risk.
             */
            case MULTIMODAL ->

                    score;
        };
    }

    private int walkingAdjustment(
            int weatherCode,
            double windSpeed
    ) {

        if (windSpeed >= 40) {
            return 12;
        }

        if (isHeavyRain(weatherCode)) {
            return 12;
        }

        return 0;
    }

    private int bicycleAdjustment(
            int weatherCode,
            double windSpeed
    ) {

        if (windSpeed >= 30) {
            return 15;
        }

        if (isRain(weatherCode)) {
            return 15;
        }

        return 0;
    }

    private int flightAdjustment(
            int weatherCode,
            double windSpeed
    ) {

        if (windSpeed >= 50) {
            return 10;
        }

        if (isStorm(weatherCode)) {
            return 15;
        }

        return 0;
    }

    private int shipAdjustment(
            int weatherCode,
            double windSpeed
    ) {

        if (windSpeed >= 30) {
            return 20;
        }

        if (isStorm(weatherCode)) {
            return 20;
        }

        return 0;
    }

    private int motorcycleAdjustment(
            int weatherCode,
            double windSpeed
    ) {

        if (windSpeed >= 30) {
            return 15;
        }

        if (isRain(weatherCode)) {
            return 15;
        }

        return 0;
    }

    public String getRecommendation(
            RiskLevel riskLevel
    ) {

        if (riskLevel == null) {

            return "Weather conditions could not be determined.";
        }

        return switch (riskLevel) {

            case HIGH ->
                    "Avoid travelling if possible. Severe weather conditions were detected along the route.";

            case MEDIUM ->
                    "Travel with caution. Weather conditions may affect parts of the route.";

            case LOW ->
                    "Weather conditions look suitable for travelling.";
        };
    }

    private boolean isSevereWeather(
            int weatherCode
    ) {

        return weatherCode == 65
                || weatherCode == 67
                || weatherCode == 75
                || weatherCode == 77
                || weatherCode == 82
                || weatherCode == 86
                || weatherCode == 95
                || weatherCode == 96
                || weatherCode == 99;
    }

    private boolean isModerateWeather(
            int weatherCode
    ) {

        return weatherCode == 51
                || weatherCode == 53
                || weatherCode == 55
                || weatherCode == 56
                || weatherCode == 57
                || weatherCode == 61
                || weatherCode == 63
                || weatherCode == 66
                || weatherCode == 71
                || weatherCode == 73
                || weatherCode == 80
                || weatherCode == 81;
    }

    private boolean isRain(
            int weatherCode
    ) {

        return weatherCode >= 51
                && weatherCode <= 67;
    }

    private boolean isHeavyRain(
            int weatherCode
    ) {

        return weatherCode == 63
                || weatherCode == 65
                || weatherCode == 67;
    }

    private boolean isStorm(
            int weatherCode
    ) {

        return weatherCode >= 95;
    }
}