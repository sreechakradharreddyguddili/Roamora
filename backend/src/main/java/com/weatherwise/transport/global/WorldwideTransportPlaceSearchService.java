package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorldwideTransportPlaceSearchService {

    private final GlobalTransportPlaceService globalTransportPlaceService;

    public WorldwideTransportPlaceSearchService(
            GlobalTransportPlaceService globalTransportPlaceService
    ) {
        this.globalTransportPlaceService = globalTransportPlaceService;
    }

    public List<TransportPlace> search(
            String query,
            String city,
            String country,
            TransportPlaceType type,
            int limit
    ) {
        return globalTransportPlaceService.search(
                query,
                city,
                country,
                type,
                limit
        );
    }

    public List<TransportPlace> search(
            String city,
            String country,
            TransportPlaceType type,
            String query,
            int limit
    ) {
        return search(query, city, country, type, limit);
    }

    public List<TransportPlace> nearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {
        return globalTransportPlaceService.nearby(
                latitude,
                longitude,
                type,
                radiusKm,
                limit
        );
    }

    public List<TransportPlace> findNearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {
        return nearby(latitude, longitude, type, radiusKm, limit);
    }
}
