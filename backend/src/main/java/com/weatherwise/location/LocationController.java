package com.weatherwise.location;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final GeocodingService geocodingService;

    public LocationController(
            GeocodingService geocodingService
    ) {
        this.geocodingService = geocodingService;
    }

    /**
     * Returns the best matching location.
     *
     * Example:
     * GET /api/v1/locations/search?city=Chennai
     */
    @GetMapping("/search")
    public ResponseEntity<LocationResponse> searchLocation(
            @RequestParam String city
    ) {

        LocationResponse response =
                geocodingService.findLocation(city);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns multiple worldwide location suggestions.
     *
     * Example:
     * GET /api/v1/locations/suggestions?query=London
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<LocationResponse>> suggestLocations(
            @RequestParam String query
    ) {

        List<LocationResponse> responses =
                geocodingService.findLocations(query);

        return ResponseEntity.ok(responses);
    }
}