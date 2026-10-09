package com.weatherwise.transport.gtfs;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import com.weatherwise.transport.TransportDataProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GtfsDataProvider
        implements TransportDataProvider {

    @Override
    public boolean supports(
            TransportPlaceType type
    ) {

        if (type == null) {
            return true;
        }

        return switch (type) {

            case BUS_STOP,
                 BUS_TERMINAL,
                 COACH_TERMINAL,
                 RAILWAY_STATION,
                 METRO_STATION,
                 SUBWAY_STATION,
                 TRAM_STOP,
                 LIGHT_RAIL_STATION,
                 FERRY_TERMINAL -> true;

            default -> false;
        };
    }

    @Override
    public List<TransportPlace> findPlaces(
            String city,
            String country,
            TransportPlaceType type
    ) {

        /*
         * Real GTFS feed ingestion will be connected here.
         *
         * Do NOT return fabricated stations/stops.
         */
        return List.of();
    }
}