package com.weatherwise.transport.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GlobalTransportDataScheduler {

    private final OurAirportsImportService importer;
    private final boolean enabled;

    public GlobalTransportDataScheduler(
            OurAirportsImportService importer,
            @Value("${app.global-transport.ourairports.enabled:true}")
            boolean enabled
    ) {
        this.importer = importer;
        this.enabled = enabled;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.global-transport.ourairports.refresh-interval-ms:86400000}",
            initialDelayString =
                    "${app.global-transport.ourairports.initial-delay-ms:120000}"
    )
    public void refreshAirports() {
        if (!enabled) return;

        try {
            int imported = importer.importAirports();
            System.out.println(
                    "Global airport refresh completed. Imported: " + imported
            );
        } catch (Exception e) {
            System.err.println(
                    "Global airport refresh failed: " + e.getMessage()
            );
        }
    }
}
