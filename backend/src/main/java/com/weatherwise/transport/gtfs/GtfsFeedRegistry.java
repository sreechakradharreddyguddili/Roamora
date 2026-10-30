package com.weatherwise.transport.gtfs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConfigurationProperties(prefix = "roamora.gtfs")
public class GtfsFeedRegistry {

    private List<GtfsFeed> feeds =
            new ArrayList<>();

    public List<GtfsFeed> findFeeds(
            String country,
            String city
    ) {

        return feeds.stream()
                .filter(GtfsFeed::isActive)
                .filter(feed ->
                        country == null
                                || country.isBlank()
                                || country.equalsIgnoreCase(
                                        feed.getCountry()
                                )
                )
                .filter(feed ->
                        city == null
                                || city.isBlank()
                                || city.equalsIgnoreCase(
                                        feed.getCity()
                                )
                )
                .toList();
    }

    public List<GtfsFeed> getAllFeeds() {
        return feeds;
    }

    public void setFeeds(
            List<GtfsFeed> feeds
    ) {

        this.feeds =
                feeds == null
                        ? new ArrayList<>()
                        : new ArrayList<>(feeds);
    }
}