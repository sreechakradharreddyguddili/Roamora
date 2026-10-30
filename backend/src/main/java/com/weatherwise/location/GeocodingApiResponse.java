package com.weatherwise.location;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GeocodingApiResponse {

    @JsonProperty("results")
    private List<LocationResult> results;

    public List<LocationResult> getResults() {
        return results;
    }

    public void setResults(List<LocationResult> results) {
        this.results = results;
    }

    public static class LocationResult {

        private String name;

        private String country;

        @JsonProperty("country_code")
        private String countryCode;

        @JsonProperty("feature_code")
        private String featureCode;

        private String admin1;

        private String admin2;

        private String admin3;

        private String admin4;

        private String timezone;

        private Integer population;

        private Double latitude;

        private Double longitude;

        @JsonProperty("elevation")
        private Double elevation;

        public String getName() {
            return name;
        }

        public String getCountry() {
            return country;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getFeatureCode() {
            return featureCode;
        }

        public String getAdmin1() {
            return admin1;
        }

        public String getAdmin2() {
            return admin2;
        }

        public String getAdmin3() {
            return admin3;
        }

        public String getAdmin4() {
            return admin4;
        }

        public String getTimezone() {
            return timezone;
        }

        public Integer getPopulation() {
            return population;
        }

        public Double getLatitude() {
            return latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public Double getElevation() {
            return elevation;
        }
    }
}