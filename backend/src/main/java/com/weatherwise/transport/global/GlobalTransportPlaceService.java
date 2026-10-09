package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GlobalTransportPlaceService {

    private final GlobalTransportPlaceRepository repository;

    public GlobalTransportPlaceService(
            GlobalTransportPlaceRepository repository
    ) {
        this.repository = repository;
    }

    public List<TransportPlace> search(
            String query,
            String city,
            String country,
            TransportPlaceType type,
            int limit
    ) {
        return repository.search(query, city, country, type, limit)
                .stream()
                .map(this::toTransportPlace)
                .toList();
    }

    public List<TransportPlace> nearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {
        return repository.nearby(
                        latitude, longitude, type, radiusKm, limit
                )
                .stream()
                .map(this::toTransportPlace)
                .toList();
    }

    private TransportPlace toTransportPlace(
            GlobalTransportPlace place
    ) {
        String publicId = place.source() + ":" + place.sourceId();

        String code = firstNonBlank(
                place.iataCode(),
                place.icaoCode(),
                place.code()
        );

        return new TransportPlace(
                publicId,
                place.name(),
                place.type(),
                place.city(),
                place.country(),
                place.countryCode(),
                place.region(),
                place.latitude(),
                place.longitude(),
                place.timezone(),
                code,
                place.operatorName(),
                place.parentSourceId(),
                place.supportedModes()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
