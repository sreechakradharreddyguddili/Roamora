package com.weatherwise.place;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
public class TransportMapPlaceController {
    private final PlaceService placeService;

    public TransportMapPlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/transport-nearby")
    public List<TransportMapPlaceResponse> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(defaultValue = "60") int limit,
            @RequestParam(required = false) String type
    ) {
        return placeService.nearbyTransportPlaces(latitude, longitude, radiusKm, limit, type);
    }
}
