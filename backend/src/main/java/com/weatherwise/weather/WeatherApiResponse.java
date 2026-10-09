package com.weatherwise.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherApiResponse {

    private double latitude;
    private double longitude;

    @JsonProperty("current")
    private CurrentWeather current;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public CurrentWeather getCurrent() {
        return current;
    }

    public static class CurrentWeather {

        @JsonProperty("temperature_2m")
        private double temperature;

        @JsonProperty("wind_speed_10m")
        private double windSpeed;

        @JsonProperty("weather_code")
        private int weatherCode;

        @JsonProperty("relative_humidity_2m")
        private double humidity;

        @JsonProperty("precipitation")
        private double precipitation;

        public double getTemperature() {
            return temperature;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getPrecipitation() {
            return precipitation;
        }
    }
}