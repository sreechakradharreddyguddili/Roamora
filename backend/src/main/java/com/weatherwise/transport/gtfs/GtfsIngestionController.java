package com.weatherwise.transport.gtfs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gtfs")
public class GtfsIngestionController {

    private final GtfsFeedRegistry gtfsFeedRegistry;
    private final GtfsIngestionService gtfsIngestionService;

    public GtfsIngestionController(
            GtfsFeedRegistry gtfsFeedRegistry,
            GtfsIngestionService gtfsIngestionService
    ) {
        this.gtfsFeedRegistry = gtfsFeedRegistry;
        this.gtfsIngestionService = gtfsIngestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(
            @RequestParam String feedId
    ) {

        try {

            GtfsFeed feed =
                    gtfsFeedRegistry
                            .getAllFeeds()
                            .stream()
                            .filter(item ->
                                    item.getFeedId()
                                            .equals(feedId)
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "GTFS feed not found: "
                                                    + feedId
                                    )
                            );

            gtfsIngestionService.ingest(feed);

            return ResponseEntity.ok(
                    "GTFS feed ingested successfully: "
                            + feedId
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "GTFS ingestion failed: "
                                    + exception.getClass().getName()
                                    + " | "
                                    + exception.getMessage()
                    );
        }
    }
}