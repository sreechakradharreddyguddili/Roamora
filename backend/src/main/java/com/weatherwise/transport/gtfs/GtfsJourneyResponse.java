package com.weatherwise.transport.gtfs;

import java.time.LocalDate;
import java.util.List;

public record GtfsJourneyResponse(
        String feedId,
        String originStopId,
        String originStopName,
        String destinationStopId,
        String destinationStopName,
        LocalDate date,
        int count,
        List<GtfsJourney> journeys
) {

    public GtfsJourneyResponse {
        journeys = journeys == null
                ? List.of()
                : List.copyOf(journeys);
    }
}