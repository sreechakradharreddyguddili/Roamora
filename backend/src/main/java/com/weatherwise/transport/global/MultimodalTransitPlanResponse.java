package com.weatherwise.transport.global;

import java.util.List;

public record MultimodalTransitPlanResponse(
        String status,
        String message,
        List<MultimodalTransitOption> options
) {
}
