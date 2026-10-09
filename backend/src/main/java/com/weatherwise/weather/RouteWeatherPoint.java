package com.weatherwise.weather;

public record RouteWeatherPoint(
        double latitude,
        double longitude,
        double temperature,
        double windSpeed,
        int weatherCode
) {
}