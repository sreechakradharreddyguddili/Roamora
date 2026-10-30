package com.weatherwise.transport.catalog;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportFeedDiscoveryService {

    private final MobilityCatalogProvider mobilityCatalogProvider;

    public TransportFeedDiscoveryService(
            MobilityCatalogProvider mobilityCatalogProvider
    ) {
        this.mobilityCatalogProvider = mobilityCatalogProvider;
    }

    public List<MobilityDatabaseFeed> discover(
            String countryCode,
            String city
    ) {

        return mobilityCatalogProvider.findFeeds(
                countryCode,
                city
        );
    }
}