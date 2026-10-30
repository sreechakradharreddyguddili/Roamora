package com.weatherwise.cache;

import com.weatherwise.dto.RouteOptionResponse;
import com.weatherwise.routing.RoutingResponse;
import com.weatherwise.weather.WeatherResponse;

import java.util.List;

public record RoutePlanCache(
        RoutingResponse routingResponse,
        WeatherResponse originWeather,
        WeatherResponse destinationWeather,
        List<RouteOptionResponse> alternatives,
        RouteOptionResponse bestRoute
) {
}