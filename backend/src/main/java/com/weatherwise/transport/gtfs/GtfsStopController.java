package com.weatherwise.transport.gtfs;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gtfs/stops")
public class GtfsStopController {
    private final GtfsRepository repository;

    public GtfsStopController(GtfsRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<GtfsStop> searchStops(
            @RequestParam String feedId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (feedId == null || feedId.isBlank()) {
            throw new IllegalArgumentException("feedId is required.");
        }
        if (query == null || query.trim().length() < 2) return List.of();
        return repository.searchStops(feedId, query, limit);
    }
}
