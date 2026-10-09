package com.weatherwise.location;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;

import java.util.List;

public record TransportPlace(
        String feedId,
        String id,
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
        String parentStationId,
        List<TransportMode> supportedModes
) {
    public TransportPlace(
            String id,
            String name,
            TransportPlaceType type,
            String city,
            String country,
            String countryCode,
            String region,
            Double latitude,
            Double longitude,
            String timezone,
            String code
    ) {
        this(
                null, id, name, type, city, country, countryCode, region,
                latitude, longitude, timezone, code, null, null, List.of()
        );
    }

    public TransportPlace(
            String id,
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
            String parentStationId,
            List<TransportMode> supportedModes
    ) {
        this(
                null, id, name, type, city, country, countryCode, region,
                latitude, longitude, timezone, code,
                operatorName, parentStationId, supportedModes
        );
    }

    public TransportPlace(
            String id,
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
            String parentStationId
    ) {
        this(
                null, id, name, type, city, country, countryCode, region,
                latitude, longitude, timezone, code,
                operatorName, parentStationId, List.of()
        );
    }

    public TransportPlace(
            String feedId,
            String id,
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
            String parentStationId,
            List<TransportMode> supportedModes
    ) {
        this.feedId = feedId;
        this.id = id;
        this.name = name;
        this.type = type;
        this.city = city;
        this.country = country;
        this.countryCode = countryCode;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
        this.code = code;
        this.operatorName = operatorName;
        this.parentStationId = parentStationId;
        this.supportedModes = supportedModes == null ? List.of() : List.copyOf(supportedModes);
    }
}
