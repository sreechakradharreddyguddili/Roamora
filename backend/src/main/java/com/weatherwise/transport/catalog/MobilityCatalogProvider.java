package com.weatherwise.transport.catalog;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MobilityCatalogProvider {

    public List<MobilityDatabaseFeed> findFeeds(
            String countryCode,
            String city
    ) {

        /*
         * The Mobility Database API requires an access token.
         *
         * Do not put a token in source code.
         *
         * This service will be connected to the API once
         * ROAMORA_MOBILITY_DATABASE_TOKEN is configured.
         */

        return List.of();
    }
}