package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GtfsFeedCatalogService {

    private final GtfsFeedRepository feedRepository;

    public GtfsFeedCatalogService(
            GtfsFeedRepository feedRepository
    ) {
        this.feedRepository = feedRepository;
    }

    public GtfsFeedCatalog buildCatalog(
            String country,
            String region,
            String city,
            Boolean active,
            Boolean realtimeEnabled
    ) {
        List<GtfsFeed> feeds = feedRepository.findAll(
                country,
                region,
                city,
                active,
                realtimeEnabled
        );

        long activeCount = feeds.stream()
                .filter(GtfsFeed::isActive)
                .count();

        long realtimeCount = feeds.stream()
                .filter(GtfsFeed::isRealtimeEnabled)
                .count();

        Map<String, CountryBuilder> countryMap =
                new LinkedHashMap<>();

        for (GtfsFeed feed : feeds) {
            if (feed == null) {
                continue;
            }

            String countryKey = key(
                    feed.getCountry(),
                    "Unknown"
            );

            CountryBuilder countryBuilder =
                    countryMap.computeIfAbsent(
                            countryKey,
                            ignored -> new CountryBuilder(
                                    display(
                                            feed.getCountry(),
                                            "Unknown"
                                    ),
                                    feed.getCountryCode()
                            )
                    );

            countryBuilder.feeds.add(feed);

            String regionKey = key(
                    feed.getRegion(),
                    "Unknown"
            );

            RegionBuilder regionBuilder =
                    countryBuilder.regions.computeIfAbsent(
                            regionKey,
                            ignored -> new RegionBuilder(
                                    display(
                                            feed.getRegion(),
                                            "Unknown"
                                    )
                            )
                    );

            regionBuilder.feeds.add(feed);

            String cityKey = key(
                    feed.getCity(),
                    "Unknown"
            );

            CityBuilder cityBuilder =
                    regionBuilder.cities.computeIfAbsent(
                            cityKey,
                            ignored -> new CityBuilder(
                                    display(
                                            feed.getCity(),
                                            "Unknown"
                                    )
                            )
                    );

            cityBuilder.feeds.add(feed);

            String providerKey = key(
                    feed.getProviderName(),
                    "Unknown provider"
            );

            OperatorBuilder operatorBuilder =
                    cityBuilder.operators.computeIfAbsent(
                            providerKey,
                            ignored -> new OperatorBuilder(
                                    display(
                                            feed.getProviderName(),
                                            "Unknown provider"
                                    )
                            )
                    );

            operatorBuilder.feeds.add(feed);
        }

        List<GtfsCountryCatalog> countries =
                countryMap.values()
                        .stream()
                        .sorted(Comparator.comparing(
                                builder -> builder.country,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toCountry)
                        .toList();

        return new GtfsFeedCatalog(
                feeds.size(),
                activeCount,
                realtimeCount,
                countries
        );
    }

    private GtfsCountryCatalog toCountry(
            CountryBuilder builder
    ) {
        List<GtfsRegionCatalog> regions =
                builder.regions.values()
                        .stream()
                        .sorted(Comparator.comparing(
                                region -> region.region,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toRegion)
                        .toList();

        long activeCount = builder.feeds.stream()
                .filter(GtfsFeed::isActive)
                .count();

        return new GtfsCountryCatalog(
                builder.country,
                builder.countryCode,
                builder.feeds.size(),
                activeCount,
                regions
        );
    }

    private GtfsRegionCatalog toRegion(
            RegionBuilder builder
    ) {
        List<GtfsCityCatalog> cities =
                builder.cities.values()
                        .stream()
                        .sorted(Comparator.comparing(
                                city -> city.city,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toCity)
                        .toList();

        return new GtfsRegionCatalog(
                builder.region,
                builder.feeds.size(),
                cities
        );
    }

    private GtfsCityCatalog toCity(
            CityBuilder builder
    ) {
        List<GtfsOperatorCatalog> operators =
                builder.operators.values()
                        .stream()
                        .sorted(Comparator.comparing(
                                operator -> operator.providerName,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .map(this::toOperator)
                        .toList();

        return new GtfsCityCatalog(
                builder.city,
                builder.feeds.size(),
                operators
        );
    }

    private GtfsOperatorCatalog toOperator(
            OperatorBuilder builder
    ) {
        List<GtfsFeed> feeds = new ArrayList<>(
                builder.feeds
        );

        feeds.sort(
                Comparator.comparing(
                        feed -> display(
                                feed.getFeedId(),
                                ""
                        ),
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        return new GtfsOperatorCatalog(
                builder.providerName,
                feeds.size(),
                feeds
        );
    }

    private String key(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback.toLowerCase()
                : value.trim().toLowerCase();
    }

    private String display(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim();
    }

    private static class CountryBuilder {
        private final String country;
        private final String countryCode;
        private final Map<String, RegionBuilder> regions =
                new LinkedHashMap<>();
        private final List<GtfsFeed> feeds =
                new ArrayList<>();

        private CountryBuilder(
                String country,
                String countryCode
        ) {
            this.country = country;
            this.countryCode = countryCode;
        }
    }

    private static class RegionBuilder {
        private final String region;
        private final Map<String, CityBuilder> cities =
                new LinkedHashMap<>();
        private final List<GtfsFeed> feeds =
                new ArrayList<>();

        private RegionBuilder(String region) {
            this.region = region;
        }
    }

    private static class CityBuilder {
        private final String city;
        private final Map<String, OperatorBuilder> operators =
                new LinkedHashMap<>();
        private final List<GtfsFeed> feeds =
                new ArrayList<>();

        private CityBuilder(String city) {
            this.city = city;
        }
    }

    private static class OperatorBuilder {
        private final String providerName;
        private final List<GtfsFeed> feeds =
                new ArrayList<>();

        private OperatorBuilder(String providerName) {
            this.providerName = providerName;
        }
    }
}
