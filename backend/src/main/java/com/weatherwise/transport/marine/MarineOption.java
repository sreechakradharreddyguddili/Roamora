package com.weatherwise.transport.marine;

import java.time.OffsetDateTime;

public record MarineOption(

        String vesselName,

        String operator,

        String originPort,
        String destinationPort,

        OffsetDateTime scheduledDeparture,
        OffsetDateTime scheduledArrival,

        OffsetDateTime estimatedDeparture,
        OffsetDateTime estimatedArrival,

        String status
) {}