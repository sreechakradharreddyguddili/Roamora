package com.weatherwise.transport.flight;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnavailableFlightScheduleProvider
        implements FlightScheduleProvider {

    @Override
    public String getProviderName() {
        return "NONE";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<FlightLeg> search(
            FlightSearchRequest request
    ) {
        return List.of();
    }
}
