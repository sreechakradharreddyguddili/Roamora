package com.weatherwise.transport;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;

import java.util.List;

public interface TransportDataProvider {

    /**
     * Returns whether this provider can currently
     * provide data for the requested place type.
     */
    boolean supports(TransportPlaceType type);

    /**
     * Finds transport places for a city/country.
     */
    List<TransportPlace> findPlaces(
            String city,
            String country,
            TransportPlaceType type
    );
}