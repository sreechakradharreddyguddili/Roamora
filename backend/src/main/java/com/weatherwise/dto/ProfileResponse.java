package com.weatherwise.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {
}