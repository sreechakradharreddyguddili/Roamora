package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import java.util.List;

public record GlobalTransportPlace(
        String source,
        String sourceId,
        String name,
        TransportPlaceType type,
        String city,
        String country,
        String countryCode,
        String region,
        Double latitude,
        Double longitude,
        String timezone,
        String code,
        String operatorName,
        String parentSourceId,
        String airportType,
        String icaoCode,
        String iataCode,
        String website,
        List<TransportMode> supportedModes
) {
    public GlobalTransportPlace {
        supportedModes = supportedModes == null ? List.of() : List.copyOf(supportedModes);
    }
}
