package com.weatherwise.transport.marine;

import java.time.OffsetDateTime;
import java.util.List;

public interface MarineDataProvider {

    List<MarineOption> search(
            String origin,
            String destination,
            OffsetDateTime departureTime
    );
}