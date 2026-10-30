package com.weatherwise.transport.gtfs;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gtfs/catalog")
public class GtfsFeedCatalogController {

    private final GtfsFeedCatalogService catalogService;

    public GtfsFeedCatalogController(
            GtfsFeedCatalogService catalogService
    ) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public GtfsFeedCatalog getCatalog(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false)
            Boolean realtimeEnabled
    ) {
        return catalogService.buildCatalog(
                country,
                region,
                city,
                active,
                realtimeEnabled
        );
    }
}
