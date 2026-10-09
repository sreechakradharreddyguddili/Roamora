package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.location.GeocodingService;
import com.weatherwise.location.LocationResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/multimodal")
public class MultimodalTransitController {

    private final GeocodingService geocodingService;
    private final MultimodalTransitPlannerService plannerService;

    public MultimodalTransitController(
            GeocodingService geocodingService,
            MultimodalTransitPlannerService plannerService
    ) {
        this.geocodingService = geocodingService;
        this.plannerService = plannerService;
    }

    @GetMapping("/transit")
    public MultimodalTransitPlanResponse plan(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "TRAIN")
            TransportMode transportMode
    ) {
        LocationResponse originLocation =
                geocodingService.findLocation(origin);

        LocationResponse destinationLocation =
                geocodingService.findLocation(destination);

        return plannerService.plan(
                originLocation,
                destinationLocation,
                transportMode
        );
    }
}
