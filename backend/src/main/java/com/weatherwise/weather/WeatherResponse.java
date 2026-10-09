package com.weatherwise.weather;

public class WeatherResponse {

    private double latitude;
    private double longitude;
    private double temperature;
    private double windSpeed;
    private int weatherCode;
    private double humidity;
    private double precipitation;

    public WeatherResponse() {
    }

    public WeatherResponse(
            double latitude,
            double longitude,
            double temperature,
            double windSpeed,
            int weatherCode
    ) {
        this(
                latitude,
                longitude,
                temperature,
                windSpeed,
                weatherCode,
                0.0,
                0.0
        );
    }

    public WeatherResponse(
            double latitude,
            double longitude,
            double temperature,
            double windSpeed,
            int weatherCode,
            double humidity,
            double precipitation
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.humidity = humidity;
        this.precipitation = precipitation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }
}