package com.weatherwise.weather;

import com.weatherwise.exception.WeatherApiException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class WeatherService {

    private final RestClient restClient;

    public WeatherService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    @Cacheable(
            value = "weather",
            key = "#latitude + ':' + #longitude"
    )
    public WeatherResponse getWeather(
            double latitude,
            double longitude
    ) {

        validateCoordinates(latitude, longitude);

        try {

            WeatherApiResponse apiResponse = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam(
                                    "current",
                                    "temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m,precipitation"
                            )
                            .build()
                    )
                    .retrieve()
                    .body(WeatherApiResponse.class);

            if (apiResponse == null
                    || apiResponse.getCurrent() == null) {

                throw new WeatherApiException(
                        "Unable to retrieve weather data"
                );
            }

            return new WeatherResponse(
                    apiResponse.getLatitude(),
                    apiResponse.getLongitude(),
                    apiResponse.getCurrent().getTemperature(),
                    apiResponse.getCurrent().getWindSpeed(),
                    apiResponse.getCurrent().getWeatherCode(),
                    apiResponse.getCurrent().getHumidity(),
                    apiResponse.getCurrent().getPrecipitation()
            );

        } catch (WeatherApiException exception) {

            throw exception;

        } catch (RestClientException exception) {

            throw new WeatherApiException(
                    "Weather API request failed: "
                            + exception.getMessage()
            );
        }
    }

    private void validateCoordinates(
            double latitude,
            double longitude
    ) {

        if (!Double.isFinite(latitude)
                || latitude < -90
                || latitude > 90) {

            throw new WeatherApiException(
                    "Latitude must be between -90 and 90"
            );
        }

        if (!Double.isFinite(longitude)
                || longitude < -180
                || longitude > 180) {

            throw new WeatherApiException(
                    "Longitude must be between -180 and 180"
            );
        }
    }
}