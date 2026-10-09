package com.weatherwise.location;

public class LocationResponse {

    private String name;
    private String country;
    private double latitude;
    private double longitude;

    public LocationResponse() {
    }

    public LocationResponse(
            String name,
            String country,
            double latitude,
            double longitude
    ) {
        this.name = name;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}