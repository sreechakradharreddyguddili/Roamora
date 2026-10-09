package com.weatherwise.transport.gtfs;

public class GtfsFeed {

    private String feedId;
    private String providerName;

    private String country;
    private String countryCode;
    private String region;
    private String city;

    private String feedUrl;

    private String realtimeTripUpdatesUrl;
    private String realtimeVehiclePositionsUrl;
    private String realtimeAlertsUrl;

    private String licenseUrl;

    private boolean active;
    private boolean realtimeEnabled;

    public GtfsFeed() {
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public String getRealtimeTripUpdatesUrl() {
        return realtimeTripUpdatesUrl;
    }

    public void setRealtimeTripUpdatesUrl(String realtimeTripUpdatesUrl) {
        this.realtimeTripUpdatesUrl = realtimeTripUpdatesUrl;
    }

    public String getRealtimeVehiclePositionsUrl() {
        return realtimeVehiclePositionsUrl;
    }

    public void setRealtimeVehiclePositionsUrl(String realtimeVehiclePositionsUrl) {
        this.realtimeVehiclePositionsUrl = realtimeVehiclePositionsUrl;
    }

    public String getRealtimeAlertsUrl() {
        return realtimeAlertsUrl;
    }

    public void setRealtimeAlertsUrl(String realtimeAlertsUrl) {
        this.realtimeAlertsUrl = realtimeAlertsUrl;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isRealtimeEnabled() {
        return realtimeEnabled;
    }

    public void setRealtimeEnabled(boolean realtimeEnabled) {
        this.realtimeEnabled = realtimeEnabled;
    }
}