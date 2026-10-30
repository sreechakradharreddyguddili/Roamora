package com.weatherwise.transport.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/global-transport/admin")
public class GlobalTransportAdminController {

    private final OurAirportsImportService ourAirportsImportService;
    private final boolean enabled;

    public GlobalTransportAdminController(
            OurAirportsImportService ourAirportsImportService,
            @Value("${app.global-transport.ourairports.enabled:true}")
            boolean enabled
    ) {
        this.ourAirportsImportService = ourAirportsImportService;
        this.enabled = enabled;
    }

    @PostMapping("/airports/refresh")
    public ResponseEntity<?> refreshAirports() {
        if (!enabled) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "disabled",
                            "message",
                            "Global airport import is disabled"
                    )
            );
        }

        int imported = ourAirportsImportService.importAirports();

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "source", "OURAIRPORTS",
                        "imported", imported
                )
        );
    }
}
