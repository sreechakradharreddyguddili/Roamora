package com.weatherwise.service;

import com.weatherwise.dto.RouteOptionResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RouteRecommendationService {

    public RouteOptionResponse selectBestRoute(
            List<RouteOptionResponse> routes
    ) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }

        double minimumDistance =
                routes.stream()
                        .map(RouteOptionResponse::getDistanceKm)
                        .filter(value -> value != null)
                        .min(Double::compareTo)
                        .orElse(0.0);

        double maximumDistance =
                routes.stream()
                        .map(RouteOptionResponse::getDistanceKm)
                        .filter(value -> value != null)
                        .max(Double::compareTo)
                        .orElse(0.0);

        int minimumDuration =
                routes.stream()
                        .map(RouteOptionResponse::getDurationMinutes)
                        .filter(value -> value != null)
                        .min(Integer::compareTo)
                        .orElse(0);

        int maximumDuration =
                routes.stream()
                        .map(RouteOptionResponse::getDurationMinutes)
                        .filter(value -> value != null)
                        .max(Integer::compareTo)
                        .orElse(0);

        return routes.stream()
                .min(
                        Comparator.comparingDouble(
                                route ->
                                        calculateCombinedScore(
                                                route,
                                                minimumDistance,
                                                maximumDistance,
                                                minimumDuration,
                                                maximumDuration
                                        )
                        )
                )
                .orElse(routes.get(0));
    }

    public double calculateCombinedScore(
            RouteOptionResponse route,
            double minimumDistance,
            double maximumDistance,
            int minimumDuration,
            int maximumDuration
    ) {
        double weatherScore =
                normalizeRiskScore(
                        route.getRiskScore()
                );

        double distanceScore =
                normalize(
                        route.getDistanceKm(),
                        minimumDistance,
                        maximumDistance
                );

        double durationScore =
                normalize(
                        route.getDurationMinutes(),
                        minimumDuration,
                        maximumDuration
                );

        return
                (weatherScore * 0.50)
                        +
                (durationScore * 0.30)
                        +
                (distanceScore * 0.20);
    }

    private double normalizeRiskScore(
            Integer riskScore
    ) {
        if (riskScore == null) {
            return 50.0;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        riskScore
                )
        );
    }

    private double normalize(
            Number value,
            Number minimum,
            Number maximum
    ) {
        if (
                value == null ||
                minimum == null ||
                maximum == null
        ) {
            return 50.0;
        }

        double current =
                value.doubleValue();

        double min =
                minimum.doubleValue();

        double max =
                maximum.doubleValue();

        if (max == min) {
            return 0.0;
        }

        return
                ((current - min) /
                        (max - min))
                        * 100.0;
    }
}