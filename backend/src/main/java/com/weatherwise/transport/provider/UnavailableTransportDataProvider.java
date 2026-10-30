package com.weatherwise.transport.provider;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.transport.TransportDataProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnavailableTransportDataProvider
        implements TransportDataProvider {

    @Override
    public boolean supports(
            TransportPlaceType type
    ) {

        return false;
    }

    @Override
    public List<TransportPlace> findPlaces(
            String city,
            String country,
            TransportPlaceType type
    ) {

        /*
         * Never invent transport locations.
         */
        return List.of();
    }
}