package com.weatherwise.transport.gtfs;

import com.weatherwise.enums.TransportMode;

public final class GtfsRouteTypeMapper {

    private GtfsRouteTypeMapper() {
    }

    public static TransportMode map(String routeType) {

        if (routeType == null || routeType.isBlank()) {
            return null;
        }

        return switch (routeType.trim()) {

            // Tram
            case "0" -> TransportMode.TRAM;

            // Subway
            case "1" -> TransportMode.SUBWAY;

            // Rail
            case "2" -> TransportMode.TRAIN;

            // Bus
            case "3" -> TransportMode.BUS;

            // Ferry
            case "4" -> TransportMode.FERRY;

            // Cable car
            case "5" -> TransportMode.MONORAIL;

            // Gondola
            case "6" -> TransportMode.MONORAIL;

            // Funicular
            case "7" -> TransportMode.TRAIN;

            default -> null;
        };
    }
}