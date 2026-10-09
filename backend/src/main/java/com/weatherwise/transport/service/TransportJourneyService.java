package com.weatherwise.transport.service;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.transport.model.TransportJourney;
import com.weatherwise.transport.model.TransportSearchResponse;
import com.weatherwise.transport.provider.TransportJourneyProvider;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransportJourneyService {

    private final List<TransportJourneyProvider> providers;

    public TransportJourneyService(
            List<TransportJourneyProvider> providers
    ) {
        this.providers =
                providers == null
                        ? List.of()
                        : providers;
    }

    public TransportSearchResponse search(
            String origin,
            String destination,
            OffsetDateTime departureTime,
            List<TransportMode> modes
    ) {

        List<TransportJourney> journeys =
                new ArrayList<>();

        for (TransportMode mode : modes) {

            for (TransportJourneyProvider provider : providers) {

                if (!provider.supports(mode)) {
                    continue;
                }

                List<TransportJourney> results =
                        provider.plan(
                                origin,
                                destination,
                                departureTime,
                                mode
                        );

                if (results != null) {
                    journeys.addAll(results);
                }
            }
        }

        if (journeys.isEmpty()) {

            return new TransportSearchResponse(
                    origin,
                    destination,
                    false,
                    "Live routing data is not available for the requested transport mode.",
                    List.of()
            );
        }

        return new TransportSearchResponse(
                origin,
                destination,
                true,
                "Transport journeys found.",
                journeys
        );
    }
}