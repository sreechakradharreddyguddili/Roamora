package com.weatherwise.transport.flight;

import java.util.List;

public interface FlightScheduleProvider {

    String getProviderName();

    boolean isAvailable();

    List<FlightLeg> search(
            FlightSearchRequest request
    );
}
