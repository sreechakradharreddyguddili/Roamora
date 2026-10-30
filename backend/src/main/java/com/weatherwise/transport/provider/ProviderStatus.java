package com.weatherwise.transport.provider;

public record ProviderStatus(
        String provider,
        boolean available,
        boolean liveDataAvailable,
        String message
) {}