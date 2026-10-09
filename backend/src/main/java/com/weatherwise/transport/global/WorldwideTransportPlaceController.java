package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/worldwide-transport-places")
public class WorldwideTransportPlaceController {

    private final WorldwideTransportPlaceSearchService service;

    public WorldwideTransportPlaceController(
            WorldwideTransportPlaceSearchService service
    ) {
        this.service = service;
    }

    /**
     * Unified worldwide search.
     *
     * Searches GTFS + global transport-place sources together.
     */
    @GetMapping
    public List<TransportPlace> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) TransportPlaceType type,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.search(
                city,
                country,
                type,
                query,
                limit
        );
    }

    @GetMapping("/nearby")
    public List<TransportPlace> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) TransportPlaceType type,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.nearby(
                latitude,
                longitude,
                type,
                radiusKm,
                limit
        );
    }
}
