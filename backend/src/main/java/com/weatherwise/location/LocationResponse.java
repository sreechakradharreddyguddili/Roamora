package com.weatherwise.location;

public class LocationResponse {

    private String name;
    private String country;
    private String region;
    private String district;
    private String featureCode;
    private double latitude;
    private double longitude;

    public LocationResponse() {
    }

    public LocationResponse(String name, String country, double latitude, double longitude) {
        this(name, country, null, null, null, latitude, longitude);
    }

    public LocationResponse(String name, String country, String region, String district, String featureCode, double latitude, double longitude) {
        this.name = name;
        this.country = country;
        this.region = region;
        this.district = district;
        this.featureCode = featureCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getRegion() { return region; }
    public String getDistrict() { return district; }
    public String getFeatureCode() { return featureCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public boolean isCountryLevel() {
        return (featureCode != null && featureCode.toUpperCase().startsWith("PCLI"))
                || (name != null && country != null && name.equalsIgnoreCase(country)
                    && (region == null || region.isBlank()) && (district == null || district.isBlank()));
    }
}
