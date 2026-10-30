package com.weatherwise.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IntelligenceChatRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 1200, message = "Question is too long")
        String question,

        @Size(max = 12000, message = "Journey context is too large")
        String journeyContext
) {
}
