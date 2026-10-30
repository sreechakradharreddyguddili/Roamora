package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class GtfsJourneyService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int DEFAULT_MAX_TRANSFERS = 3;

    private static final int DEFAULT_MIN_TRANSFER_MINUTES = 5;

    private final GtfsJourneyRepository journeyRepository;

    private final GtfsMultiLegJourneyService multiLegJourneyService;

    private final GtfsServiceCalendarService serviceCalendarService;

    private final GtfsJourneyRealtimeEnricher realtimeEnricher;

    public GtfsJourneyService(
            GtfsJourneyRepository journeyRepository,
            GtfsMultiLegJourneyService multiLegJourneyService,
            GtfsServiceCalendarService serviceCalendarService,
            GtfsJourneyRealtimeEnricher realtimeEnricher
    ) {

        this.journeyRepository =
                journeyRepository;

        this.multiLegJourneyService =
                multiLegJourneyService;

        this.serviceCalendarService =
                serviceCalendarService;

        this.realtimeEnricher =
                realtimeEnricher;
    }

    /*
     * ============================================================
     * LEGACY DIRECT JOURNEY METHODS
     * ============================================================
     */

    public List<GtfsJourney> findJourneys(
            String feedId,
            String originStopId,
            String destinationStopId,
            int limit
    ) {

        validate(
                feedId,
                originStopId,
                destinationStopId
        );

        return journeyRepository.findJourneys(
                feedId,
                originStopId,
                destinationStopId,
                normalizeLimit(limit)
        );
    }

    public List<GtfsJourney> findJourneys(
            String feedId,
            String originStopId,
            String destinationStopId
    ) {

        return findJourneys(
                feedId,
                originStopId,
                destinationStopId,
                DEFAULT_LIMIT
        );
    }

    /*
     * ============================================================
     * DATE-AWARE JOURNEYS
     * ============================================================
     */

    public List<GtfsJourney> findJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int limit
    ) {

        validate(
                feedId,
                originStopId,
                destinationStopId
        );

        validateDate(date);

        int safeLimit =
                normalizeLimit(limit);

        List<GtfsJourney> journeys =
                multiLegJourneyService.findJourneys(
                        feedId,
                        originStopId,
                        destinationStopId,
                        date,
                        DEFAULT_MAX_TRANSFERS,
                        DEFAULT_MIN_TRANSFER_MINUTES,
                        safeLimit
                );

        return realtimeEnricher.enrich(
                journeys
        );
    }

    public List<GtfsJourney> findJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
    ) {

        return findJourneysForDate(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_LIMIT
        );
    }

    /*
     * ============================================================
     * CONFIGURABLE MULTI-LEG JOURNEYS
     * ============================================================
     */

    public List<GtfsJourney> findJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int maxTransfers,
            int minimumTransferMinutes,
            int limit
    ) {

        validate(
                feedId,
                originStopId,
                destinationStopId
        );

        validateDate(date);

        int safeMaxTransfers =
                Math.max(
                        0,
                        Math.min(
                                maxTransfers,
                                6
                        )
                );

        int safeTransferMinutes =
                Math.max(
                        0,
                        Math.min(
                                minimumTransferMinutes,
                                180
                        )
                );

        int safeLimit =
                normalizeLimit(limit);

        List<GtfsJourney> journeys =
                multiLegJourneyService.findJourneys(
                        feedId,
                        originStopId,
                        destinationStopId,
                        date,
                        safeMaxTransfers,
                        safeTransferMinutes,
                        safeLimit
                );

        return realtimeEnricher.enrich(
                journeys
        );
    }

    /*
     * ============================================================
     * TRANSFER-AWARE API
     * ============================================================
     */

    public List<GtfsJourney> findJourneysWithTransfersForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int limit
    ) {

        return findJourneysForDate(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_MAX_TRANSFERS,
                DEFAULT_MIN_TRANSFER_MINUTES,
                limit
        );
    }

    public List<GtfsJourney> findJourneysWithTransfersForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
    ) {

        return findJourneysWithTransfersForDate(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_LIMIT
        );
    }

    /*
     * ============================================================
     * DIRECT JOURNEYS FOR A SPECIFIC DATE
     * ============================================================
     */

    public List<GtfsJourney> findDirectJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int limit
    ) {

        validate(
                feedId,
                originStopId,
                destinationStopId
        );

        validateDate(date);

        Set<String> activeServices =
                serviceCalendarService.findActiveServices(
                        feedId,
                        date
                );

        if (activeServices.isEmpty()) {
            return List.of();
        }

        List<GtfsJourney> journeys =
                journeyRepository
                        .findJourneysForActiveServices(
                                feedId,
                                originStopId,
                                destinationStopId,
                                activeServices.stream()
                                        .filter(id ->
                                                id != null &&
                                                        !id.isBlank()
                                        )
                                        .distinct()
                                        .toList(),
                                normalizeLimit(limit)
                        );

        return realtimeEnricher.enrich(
                journeys
        );
    }

    public List<GtfsJourney> findDirectJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
    ) {

        return findDirectJourneysForDate(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_LIMIT
        );
    }

    /*
     * ============================================================
     * OLD ONE-TRANSFER COMPATIBILITY METHOD
     * ============================================================
     */

    public List<GtfsJourney> findOneTransferJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int minimumTransferMinutes,
            int limit
    ) {

        validate(
                feedId,
                originStopId,
                destinationStopId
        );

        validateDate(date);

        Set<String> activeServices =
                serviceCalendarService.findActiveServices(
                        feedId,
                        date
                );

        if (activeServices.isEmpty()) {
            return List.of();
        }

        List<GtfsJourney> journeys =
                journeyRepository
                        .findOneTransferJourneysForActiveServices(
                                feedId,
                                originStopId,
                                destinationStopId,
                                activeServices.stream()
                                        .filter(id ->
                                                id != null &&
                                                        !id.isBlank()
                                        )
                                        .distinct()
                                        .toList(),
                                Math.max(
                                        0,
                                        Math.min(
                                                minimumTransferMinutes,
                                                180
                                        )
                                ),
                                normalizeLimit(limit)
                        );

        return realtimeEnricher.enrich(
                journeys
        );
    }

    public List<GtfsJourney> findOneTransferJourneysForDate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
    ) {

        return findOneTransferJourneysForDate(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_MIN_TRANSFER_MINUTES,
                DEFAULT_LIMIT
        );
    }

    /*
     * ============================================================
     * ACTIVE SERVICE INFORMATION
     * ============================================================
     */

    public Set<String> findActiveServices(
            String feedId,
            LocalDate date
    ) {

        if (feedId == null ||
                feedId.isBlank()) {

            throw new IllegalArgumentException(
                    "Feed ID is required"
            );
        }

        validateDate(date);

        return serviceCalendarService.findActiveServices(
                feedId,
                date
        );
    }

    /*
     * ============================================================
     * VALIDATION
     * ============================================================
     */

    private void validate(
            String feedId,
            String originStopId,
            String destinationStopId
    ) {

        if (feedId == null ||
                feedId.isBlank()) {

            throw new IllegalArgumentException(
                    "Feed ID is required"
            );
        }

        if (originStopId == null ||
                originStopId.isBlank()) {

            throw new IllegalArgumentException(
                    "Origin stop ID is required"
            );
        }

        if (destinationStopId == null ||
                destinationStopId.isBlank()) {

            throw new IllegalArgumentException(
                    "Destination stop ID is required"
            );
        }

        if (originStopId.equals(
                destinationStopId
        )) {

            throw new IllegalArgumentException(
                    "Origin and destination stops must be different"
            );
        }
    }

    private void validateDate(
            LocalDate date
    ) {

        if (date == null) {

            throw new IllegalArgumentException(
                    "Date is required"
            );
        }
    }

    private int normalizeLimit(
            int limit
    ) {

        return Math.max(
                1,
                Math.min(
                        limit,
                        50
                )
        );
    }
}