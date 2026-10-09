package com.weatherwise.location;

import com.weatherwise.exception.LocationNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class GeocodingService {

    private final RestClient restClient;

    public GeocodingService(
            RestClient.Builder restClientBuilder
    ) {

        this.restClient =
                restClientBuilder
                        .baseUrl(
                                "https://geocoding-api.open-meteo.com"
                        )
                        .defaultHeader(
                                "User-Agent",
                                "Roamora-Journey-Intelligence/1.0"
                        )
                        .build();
    }

    /**
     * Resolves a single location.
     *
     * This preserves the existing /search behavior.
     */
    @Cacheable(
            value = "locations",
            key = "#location.trim().toLowerCase()",
            unless = "#result == null"
    )
    public LocationResponse findLocation(
            String location
    ) {

        if (
                location == null
                        || location.trim().isEmpty()
        ) {

            throw new LocationNotFoundException(
                    "Location must not be empty."
            );
        }

        String normalizedLocation =
                location.trim();

        try {

            GeocodingApiResponse response =
                    requestLocations(normalizedLocation);

            if (
                    response == null
                            || response.getResults() == null
                            || response.getResults().isEmpty()
            ) {

                throw new LocationNotFoundException(
                        "Location not found: "
                                + normalizedLocation
                );
            }

            /*
             * Prefer an exact name match.
             */
            GeocodingApiResponse.LocationResult selected =
                    findExactMatch(
                            response,
                            normalizedLocation
                    );

            /*
             * If there is no exact match,
             * use the first valid geographic result.
             */
            if (selected == null) {

                selected =
                        response
                                .getResults()
                                .stream()
                                .filter(
                                        result ->
                                                result != null
                                                        && result.getLatitude() != null
                                                        && result.getLongitude() != null
                                )
                                .findFirst()
                                .orElse(null);
            }

            if (selected == null) {

                throw new LocationNotFoundException(
                        "No usable geographic coordinates found for: "
                                + normalizedLocation
                );
            }

            return toLocationResponse(selected);

        } catch (
                LocationNotFoundException exception
        ) {

            throw exception;

        } catch (
                RestClientException exception
        ) {

            throw new LocationNotFoundException(
                    "Unable to retrieve geographic data for: "
                            + normalizedLocation
            );
        }
    }

    /**
     * Returns multiple worldwide suggestions.
     */
    @Cacheable(
            value = "locationSuggestions",
            key = "#query.trim().toLowerCase()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<LocationResponse> findLocations(
            String query
    ) {

        if (
                query == null
                        || query.trim().isEmpty()
        ) {

            return List.of();
        }

        String normalizedQuery =
                query.trim();

        try {

            GeocodingApiResponse response =
                    requestLocations(normalizedQuery);

            if (
                    response == null
                            || response.getResults() == null
            ) {

                return List.of();
            }

            return response
                    .getResults()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(
                            result ->
                                    result.getName() != null
                                            && result.getLatitude() != null
                                            && result.getLongitude() != null
                    )
                    .map(this::toLocationResponse)
                    .distinct()
                    .limit(10)
                    .toList();

        } catch (RestClientException exception) {

            /*
             * Autocomplete should fail gracefully.
             * The user can still type the location manually.
             */
            return List.of();
        }
    }

    /**
     * Calls Open-Meteo geocoding.
     */
    private GeocodingApiResponse requestLocations(
            String location
    ) {

        return restClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/v1/search")
                                        .queryParam(
                                                "name",
                                                location
                                        )
                                        .queryParam(
                                                "count",
                                                10
                                        )
                                        .queryParam(
                                                "language",
                                                "en"
                                        )
                                        .queryParam(
                                                "format",
                                                "json"
                                        )
                                        .build()
                )
                .retrieve()
                .body(
                        GeocodingApiResponse.class
                );
    }

    /**
     * Converts the Open-Meteo result
     * into our application DTO.
     */
    private LocationResponse toLocationResponse(
            GeocodingApiResponse.LocationResult result
    ) {

        return new LocationResponse(
                result.getName(),
                result.getCountry(),
                result.getLatitude(),
                result.getLongitude()
        );
    }

    /**
     * Finds an exact geographic name match.
     */
    private GeocodingApiResponse.LocationResult findExactMatch(
            GeocodingApiResponse response,
            String requestedLocation
    ) {

        if (
                response.getResults() == null
                        || response.getResults().isEmpty()
        ) {

            return null;
        }

        String normalizedRequested =
                requestedLocation
                        .trim()
                        .toLowerCase(Locale.ROOT);

        for (
                GeocodingApiResponse.LocationResult result
                : response.getResults()
        ) {

            if (
                    result == null
                            || result.getName() == null
            ) {

                continue;
            }

            String resultName =
                    result.getName()
                            .trim()
                            .toLowerCase(Locale.ROOT);

            if (
                    resultName.equals(
                            normalizedRequested
                    )
            ) {

                return result;
            }
        }

        return null;
    }
}