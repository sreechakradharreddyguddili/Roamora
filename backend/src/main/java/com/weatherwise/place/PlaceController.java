package com.weatherwise.place;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PlaceResponse>> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) PlaceCategory category,
            @RequestParam(defaultValue = "8") @Min(1) @Max(25) double radiusKm,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(
                placeService.nearby(latitude, longitude, category, radiusKm, limit)
        );
    }
}
