package com.weatherwise.transport.model;

import com.weatherwise.enums.TransportMode;

import java.time.OffsetDateTime;
import java.util.List;

public record TransportSearchRequest(
        String origin,
        String destination,

        OffsetDateTime departureTime,

        List<TransportMode> modes,

        Integer maxTransfers,

        Boolean wheelchairAccessible,

        Boolean bicycleAllowed
) {}