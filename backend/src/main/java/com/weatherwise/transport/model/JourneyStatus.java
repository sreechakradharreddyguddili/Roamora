package com.weatherwise.transport.model;

public enum JourneyStatus {

    SCHEDULED,
    BOARDING,
    DEPARTED,
    EN_ROUTE,
    ARRIVING,
    ARRIVED,

    DELAYED,
    CANCELED,
    SKIPPED,

    UNKNOWN,

    DATA_UNAVAILABLE
}