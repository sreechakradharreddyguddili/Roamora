package com.weatherwise.transport.gtfs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gtfs/feeds")
public class GtfsFeedController {

    private final GtfsFeedService feedService;

    public GtfsFeedController(
            GtfsFeedService feedService
    ) {
        this.feedService = feedService;
    }

    @GetMapping
    public List<GtfsFeed> findFeeds(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false)
            Boolean realtimeEnabled
    ) {
        return feedService.findFeeds(
                country,
                region,
                city,
                active,
                realtimeEnabled
        );
    }

    @GetMapping("/{feedId}")
    public GtfsFeed findFeed(
            @PathVariable String feedId
    ) {
        return feedService.findFeed(feedId);
    }

    @PostMapping
    public ResponseEntity<GtfsFeed> create(
            @RequestBody GtfsFeedRequest request
    ) {
        GtfsFeed created =
                feedService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{feedId}")
    public GtfsFeed update(
            @PathVariable String feedId,
            @RequestBody GtfsFeedRequest request
    ) {
        return feedService.update(
                feedId,
                request
        );
    }

    @PatchMapping("/{feedId}/active")
    public GtfsFeed setActive(
            @PathVariable String feedId,
            @RequestParam boolean active
    ) {
        return feedService.setActive(
                feedId,
                active
        );
    }

    @PatchMapping("/{feedId}/realtime")
    public GtfsFeed setRealtimeEnabled(
            @PathVariable String feedId,
            @RequestParam boolean enabled
    ) {
        return feedService.setRealtimeEnabled(
                feedId,
                enabled
        );
    }

    @PostMapping("/{feedId}/refresh")
    public GtfsRealtimeRefreshResult refreshRealtime(
            @PathVariable String feedId
    ) {
        return feedService.refreshRealtime(
                feedId
        );
    }

    @PostMapping("/{feedId}/ingest")
    public ResponseEntity<Void> ingest(
            @PathVariable String feedId
    ) {
        feedService.ingestFeed(feedId);

        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{feedId}")
    public GtfsFeed deactivate(
            @PathVariable String feedId
    ) {
        return feedService.deactivate(feedId);
    }
}
