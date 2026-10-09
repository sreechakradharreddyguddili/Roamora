package com.weatherwise.location;

import com.weatherwise.enums.TransportPlaceType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transport-places")
public class TransportPlaceController {

    private final TransportPlaceService transportPlaceService;

    public TransportPlaceController(
            TransportPlaceService transportPlaceService
    ) {
        this.transportPlaceService =
                transportPlaceService;
    }

    /**
     * Search transport places.
     *
     * Examples:
     *
     * /api/v1/transport-places?city=Delhi
     *
     * /api/v1/transport-places?city=Delhi&country=India
     *
     * /api/v1/transport-places
     * ?city=Delhi
     * &country=India
     * &type=METRO_STATION
     *
     * /api/v1/transport-places
     * ?city=Delhi
     * &country=India
     * &query=Rajiv
     */
    @GetMapping
    public TransportPlaceResponse findPlaces(

            @RequestParam
            String city,

            @RequestParam(
                    required = false
            )
            String country,

            @RequestParam(
                    required = false
            )
            TransportPlaceType type,

            @RequestParam(
                    required = false
            )
            String query
    ) {

        if (query != null
                && !query.isBlank()) {

            return transportPlaceService.searchPlaces(
                    city,
                    country,
                    type,
                    query
            );
        }

        return transportPlaceService.findPlaces(
                city,
                country,
                type
        );
    }

    /**
     * Find transport places near coordinates.
     *
     * Example:
     *
     * /api/v1/transport-places/nearby
     * ?latitude=51.674
     * &longitude=-0.607
     * &type=RAILWAY_STATION
     * &radiusKm=10
     * &limit=50
     */
    @GetMapping("/nearby")
    public TransportPlaceResponse findNearby(

            @RequestParam
            double latitude,

            @RequestParam
            double longitude,

            @RequestParam(
                    required = false
            )
            TransportPlaceType type,

            @RequestParam(
                    defaultValue = "10"
            )
            double radiusKm,

            @RequestParam(
                    defaultValue = "50"
            )
            int limit
    ) {

        return transportPlaceService.findNearby(
                latitude,
                longitude,
                type,
                radiusKm,
                limit
        );
    }
}