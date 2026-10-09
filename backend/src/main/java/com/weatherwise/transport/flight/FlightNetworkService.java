package com.weatherwise.transport.flight;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightNetworkService {

    private final List<FlightScheduleProvider> providers;

    public FlightNetworkService(
            List<FlightScheduleProvider> providers
    ) {
        this.providers =
                providers == null
                        ? List.of()
                        : List.copyOf(providers);
    }

    public FlightSearchResponse search(
            FlightSearchRequest request
    ) {
        validate(request);

        for (FlightScheduleProvider provider : providers) {
            if (provider == null
                    || !provider.isAvailable()) {
                continue;
            }

            try {
                List<FlightLeg> flights =
                        provider.search(request);

                if (flights == null
                        || flights.isEmpty()) {
                    continue;
                }

                return FlightSearchResponse.available(
                        provider.getProviderName(),
                        flights
                );
            } catch (Exception ignored) {
                /*
                 * One provider failure must not cause
                 * the entire travel planner to fail.
                 */
            }
        }

        return FlightSearchResponse.unavailable(
                "Live flight schedule data is not configured. " +
                        "No flight route has been fabricated."
        );
    }

    public boolean hasAvailableProvider() {
        return providers.stream()
                .anyMatch(
                        provider ->
                                provider != null
                                        && provider.isAvailable()
                );
    }

    private void validate(
            FlightSearchRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Flight search request is required."
            );
        }

        if (request.departureDate() == null) {
            throw new IllegalArgumentException(
                    "Departure date is required."
            );
        }

        boolean hasOrigin =
                request.originAirportCode() != null
                        || request.originAirportId() != null;

        boolean hasDestination =
                request.destinationAirportCode() != null
                        || request.destinationAirportId() != null;

        if (!hasOrigin || !hasDestination) {
            throw new IllegalArgumentException(
                    "Origin and destination airport identifiers are required."
            );
        }
    }
}
