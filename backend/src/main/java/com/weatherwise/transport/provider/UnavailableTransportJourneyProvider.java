package com.weatherwise.transport.provider;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.transport.model.TransportJourney;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class UnavailableTransportJourneyProvider
        implements TransportJourneyProvider {

    @Override
    public boolean supports(TransportMode mode) {

        return switch (mode) {

            case FLIGHT,
                 FERRY,
                 SHIP -> true;

            default -> false;
        };
    }

    @Override
    public List<TransportJourney> plan(
            String origin,
            String destination,
            OffsetDateTime departureTime,
            TransportMode mode
    ) {

        return List.of();
    }
}