package com.weatherwise.location;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.transport.gtfs.GtfsSearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportPlaceService {

    private final GtfsSearchRepository gtfsSearchRepository;

    public TransportPlaceService(
            GtfsSearchRepository gtfsSearchRepository
    ) {
        this.gtfsSearchRepository =
                gtfsSearchRepository;
    }

    public TransportPlaceResponse findPlaces(
            String city,
            String country,
            TransportPlaceType type
    ) {

        if (city == null || city.isBlank()) {

            return new TransportPlaceResponse(
                    city,
                    city,
                    country,
                    List.of()
            );
        }

        String normalizedCity =
                city.trim();

        List<TransportPlace> places =
                gtfsSearchRepository.searchPlaces(
                        normalizedCity,
                        country,
                        type,
                        null
                );

        return new TransportPlaceResponse(
                normalizedCity,
                normalizedCity,
                country,
                places
        );
    }

    public TransportPlaceResponse searchPlaces(
            String city,
            String country,
            TransportPlaceType type,
            String query
    ) {

        if (city == null || city.isBlank()) {

            return new TransportPlaceResponse(
                    city,
                    city,
                    country,
                    List.of()
            );
        }

        String normalizedCity =
                city.trim();

        List<TransportPlace> places =
                gtfsSearchRepository.searchPlaces(
                        normalizedCity,
                        country,
                        type,
                        query
                );

        return new TransportPlaceResponse(
                normalizedCity,
                normalizedCity,
                country,
                places
        );
    }

    public TransportPlaceResponse findPlaces(
            String city,
            String country
    ) {

        return findPlaces(
                city,
                country,
                null
        );
    }

    public TransportPlaceResponse findNearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {

        List<TransportPlace> places =
                gtfsSearchRepository.searchNearby(
                        latitude,
                        longitude,
                        type,
                        radiusKm,
                        limit
                );

        return new TransportPlaceResponse(
                null,
                null,
                null,
                places
        );
    }
}