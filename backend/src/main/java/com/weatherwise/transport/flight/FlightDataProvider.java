package com.weatherwise.transport.flight;

import java.time.OffsetDateTime;
import java.util.List;

public interface FlightDataProvider {

    List<FlightOption> search(
            String originAirportCode,
            String destinationAirportCode,
            OffsetDateTime departureTime
    );
}