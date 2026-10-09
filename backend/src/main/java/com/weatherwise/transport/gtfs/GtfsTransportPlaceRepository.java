package com.weatherwise.transport.gtfs;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GtfsTransportPlaceRepository {

    private final GtfsSearchRepository gtfsSearchRepository;

    public GtfsTransportPlaceRepository(
            GtfsSearchRepository gtfsSearchRepository
    ) {
        this.gtfsSearchRepository =
                gtfsSearchRepository;
    }

    public List<GtfsTransportPlaceResult> search(
            String city,
            String country,
            TransportPlaceType type,
            String query,
            int limit
    ) {

        int safeLimit =
                Math.max(
                        1,
                        Math.min(limit, 100)
                );

        return gtfsSearchRepository
                .searchPlaces(
                        city,
                        country,
                        type,
                        query
                )
                .stream()
                .limit(safeLimit)
                .map(this::toResult)
                .toList();
    }

    public List<GtfsTransportPlaceResult> nearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {

        int safeLimit =
                Math.max(
                        1,
                        Math.min(limit, 100)
                );

        return gtfsSearchRepository
                .searchNearby(
                        latitude,
                        longitude,
                        type,
                        radiusKm,
                        safeLimit
                )
                .stream()
                .map(this::toResult)
                .toList();
    }

    private GtfsTransportPlaceResult toResult(
            TransportPlace place
    ) {

        return new GtfsTransportPlaceResult(
                place.id(),
                place.name(),
                place.code(),
                place.latitude(),
                place.longitude(),
                resolveLocationType(place.type()),
                place.parentStationId(),
                place.timezone(),
                place.city(),
                place.country(),
                place.countryCode(),
                place.region()
        );
    }

    private String resolveLocationType(
            TransportPlaceType type
    ) {

        if (type == null) {
            return null;
        }

        return switch (type) {

            case AIRPORT ->
                    "AIRPORT";

            case RAILWAY_STATION ->
                    "RAILWAY_STATION";

            case METRO_STATION ->
                    "METRO_STATION";

            case SUBWAY_STATION ->
                    "SUBWAY_STATION";

            case TRAM_STOP ->
                    "TRAM_STOP";

            case LIGHT_RAIL_STATION ->
                    "LIGHT_RAIL_STATION";

            case MONORAIL_STATION ->
                    "MONORAIL_STATION";

            case BUS_STOP ->
                    "BUS_STOP";

            case BUS_TERMINAL ->
                    "BUS_TERMINAL";

            case COACH_STOP ->
                    "COACH_STOP";

            case COACH_TERMINAL ->
                    "COACH_TERMINAL";

            case FERRY_TERMINAL ->
                    "FERRY_TERMINAL";

            case PORT ->
                    "PORT";

            case TAXI_STAND ->
                    "TAXI_STAND";

            case CAR_RENTAL ->
                    "CAR_RENTAL";

            case BICYCLE_STATION ->
                    "BICYCLE_STATION";

            case SCOOTER_STATION ->
                    "SCOOTER_STATION";
        };
    }
}