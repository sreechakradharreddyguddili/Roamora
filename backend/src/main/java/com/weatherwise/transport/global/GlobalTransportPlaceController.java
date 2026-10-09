package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/global-transport-places")
public class GlobalTransportPlaceController {

    private final GlobalTransportPlaceService service;

    public GlobalTransportPlaceController(
            GlobalTransportPlaceService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<TransportPlace> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) TransportPlaceType type,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.search(query, city, country, type, limit);
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
                latitude, longitude, type, radiusKm, limit
        );
    }
}
