package com.weatherwise.transport.provider;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.transport.model.TransportJourney;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransportJourneyProvider {

    boolean supports(TransportMode mode);

    List<TransportJourney> plan(
            String origin,
            String destination,
            OffsetDateTime departureTime,
            TransportMode mode
    );
}