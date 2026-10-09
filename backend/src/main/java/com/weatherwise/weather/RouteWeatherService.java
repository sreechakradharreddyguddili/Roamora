package com.weatherwise.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteWeatherService {

    private static final int MAX_WEATHER_POINTS = 8;

    private final WeatherService weatherService;

    private final ObjectMapper objectMapper;

    public RouteWeatherService(
            WeatherService weatherService,
            ObjectMapper objectMapper
    ) {
        this.weatherService = weatherService;
        this.objectMapper = objectMapper;
    }

    public List<RouteWeatherPoint> getWeatherAlongRoute(
            String geometry
    ) {

        List<Coordinate> coordinates =
                extractCoordinates(geometry);

        if (coordinates.isEmpty()) {
            return List.of();
        }

        List<Coordinate> sampledCoordinates =
                sampleCoordinates(coordinates);

        List<RouteWeatherPoint> weatherPoints =
                new ArrayList<>();

        for (Coordinate coordinate :
                sampledCoordinates) {

            WeatherResponse weather =
                    weatherService.getWeather(
                            coordinate.latitude(),
                            coordinate.longitude()
                    );

            weatherPoints.add(
                    new RouteWeatherPoint(
                            coordinate.latitude(),
                            coordinate.longitude(),
                            weather.getTemperature(),
                            weather.getWindSpeed(),
                            weather.getWeatherCode()
                    )
            );
        }

        return weatherPoints;
    }

    private List<Coordinate> extractCoordinates(
            String geometry
    ) {

        if (
                geometry == null ||
                geometry.isBlank()
        ) {
            return List.of();
        }

        try {

            Object parsed =
                    objectMapper.readValue(
                            geometry,
                            Object.class
                    );

            return extractFromObject(parsed);

        } catch (Exception exception) {

            return List.of();
        }
    }

    private List<Coordinate> extractFromObject(
            Object object
    ) {

        if (!(object instanceof java.util.Map<?, ?> map)) {
            return List.of();
        }

        Object type =
                map.get("type");

        if (
                "FeatureCollection"
                        .equals(type)
        ) {

            Object features =
                    map.get("features");

            if (!(features instanceof List<?> featureList)) {
                return List.of();
            }

            for (Object feature :
                    featureList) {

                List<Coordinate> result =
                        extractFromObject(feature);

                if (!result.isEmpty()) {
                    return result;
                }
            }

            return List.of();
        }

        if ("Feature".equals(type)) {

            return extractFromObject(
                    map.get("geometry")
            );
        }

        Object coordinates =
                map.get("coordinates");

        if (coordinates == null) {
            return List.of();
        }

        List<Coordinate> result =
                new ArrayList<>();

        flattenCoordinates(
                coordinates,
                result
        );

        return result;
    }

    private void flattenCoordinates(
            Object value,
            List<Coordinate> result
    ) {

        if (!(value instanceof List<?> list)) {
            return;
        }

        if (isCoordinatePair(list)) {

            double longitude =
                    ((Number) list.get(0))
                            .doubleValue();

            double latitude =
                    ((Number) list.get(1))
                            .doubleValue();

            result.add(
                    new Coordinate(
                            latitude,
                            longitude
                    )
            );

            return;
        }

        for (Object child :
                list) {

            flattenCoordinates(
                    child,
                    result
            );
        }
    }

    private boolean isCoordinatePair(
            List<?> list
    ) {

        return list.size() >= 2
                && list.get(0) instanceof Number
                && list.get(1) instanceof Number;
    }

    private List<Coordinate> sampleCoordinates(
            List<Coordinate> coordinates
    ) {

        if (
                coordinates.size()
                        <= MAX_WEATHER_POINTS
        ) {
            return coordinates;
        }

        List<Coordinate> sampled =
                new ArrayList<>();

        double step =
                (double)
                        (coordinates.size() - 1)
                        /
                        (MAX_WEATHER_POINTS - 1);

        for (
                int i = 0;
                i < MAX_WEATHER_POINTS;
                i++
        ) {

            int index =
                    (int)
                            Math.round(
                                    i * step
                            );

            index =
                    Math.min(
                            index,
                            coordinates.size() - 1
                    );

            sampled.add(
                    coordinates.get(index)
            );
        }

        return sampled;
    }

    private record Coordinate(
            double latitude,
            double longitude
    ) {
    }
}