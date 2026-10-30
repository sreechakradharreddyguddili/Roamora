package com.weatherwise.transport.gtfs;

import java.util.List;

public record GtfsFeedResponse(

        String country,

        String city,

        List<GtfsFeed> feeds

) {
}