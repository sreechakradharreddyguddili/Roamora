package com.weatherwise.transport.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/global-transport/admin")
public class GlobalTransitAdminController {

    private final OpenStreetMapTransitImportService importer;
    private final boolean enabled;

    public GlobalTransitAdminController(
            OpenStreetMapTransitImportService importer,
            @Value("${app.global-transport.osm.enabled:true}")
            boolean enabled
    ) {
        this.importer = importer;
        this.enabled = enabled;
    }

    @PostMapping("/osm/refresh")
    public ResponseEntity<?> refresh(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "15") double radiusKm
    ) {
        if (!enabled) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "disabled",
                            "message",
                            "OpenStreetMap transit import is disabled"
                    )
            );
        }

        int imported =
                importer.importArea(
                        latitude,
                        longitude,
                        radiusKm
                );

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "source", "OSM",
                        "imported", imported,
                        "latitude", latitude,
                        "longitude", longitude,
                        "radiusKm", radiusKm
                )
        );
    }
}
