package com.weatherwise.transport.global;

import com.weatherwise.location.TransportPlace;
import com.weatherwise.transport.gtfs.GtfsJourney;

public record MultimodalTransitOption(
        String optionId,
        TransportPlace boardingPlace,
        TransportPlace alightingPlace,
        MultimodalConnector access,
        GtfsJourney journey,
        MultimodalConnector egress,
        Double totalKnownDistanceKm,
        Integer totalKnownDurationMinutes,
        String availability
) {
}
