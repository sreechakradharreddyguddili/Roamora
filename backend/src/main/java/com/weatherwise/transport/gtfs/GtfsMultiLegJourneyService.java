package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
public class GtfsMultiLegJourneyService {

    private static final int DEFAULT_MAX_TRANSFERS = 3;

    private static final int DEFAULT_MIN_TRANSFER_MINUTES = 5;

    private static final int CONNECTION_BATCH_SIZE = 250;

    private final GtfsTransitGraphRepository graphRepository;

    private final GtfsServiceCalendarService serviceCalendarService;

    public GtfsMultiLegJourneyService(
            GtfsTransitGraphRepository graphRepository,
            GtfsServiceCalendarService serviceCalendarService
    ) {
        this.graphRepository =
                graphRepository;

        this.serviceCalendarService =
                serviceCalendarService;
    }

    /**
     * Finds multimodal GTFS journeys for a specific date.
     *
     * Supports:
     *
     *     A -> D
     *
     *     A -> B -> D
     *
     *     A -> B -> C -> D
     *
     *     A -> B -> C -> D -> E
     *
     * The number of transfers is controlled by maxTransfers.
     */
    public List<GtfsJourney> findJourneys(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date,
            int limit
    ) {

        return findJourneys(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_MAX_TRANSFERS,
                DEFAULT_MIN_TRANSFER_MINUTES,
                limit
        );
    }

    /**
     * Full configurable journey search.
     */
    public List<GtfsJourney> findJourneys(
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
                destinationStopId,
                date
        );

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
                Math.max(
                        1,
                        Math.min(
                                limit,
                                50
                        )
                );

        Set<String> activeServices =
                serviceCalendarService.findActiveServices(
                        feedId,
                        date
                );

        if (activeServices.isEmpty()) {
            return List.of();
        }

        List<String> serviceIds =
                activeServices.stream()
                        .filter(id ->
                                id != null &&
                                !id.isBlank()
                        )
                        .distinct()
                        .toList();

        if (serviceIds.isEmpty()) {
            return List.of();
        }

        /*
         * Priority queue makes this a time-dependent
         * earliest-arrival search.
         *
         * Earlier arrivals are processed first.
         */
        PriorityQueue<SearchState> queue =
                new PriorityQueue<>(
                        Comparator.comparing(
                                SearchState::arrivalTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                );

        /*
         * We don't have a requested departure time yet.
         *
         * Therefore we start with all useful connections leaving
         * the origin stop.
         */
        List<GtfsTransitConnection> firstConnections =
                graphRepository.findConnectionsFromStop(
                        feedId,
                        originStopId,
                        serviceIds,
                        CONNECTION_BATCH_SIZE
                );

        if (firstConnections.isEmpty()) {
            return List.of();
        }

        for (GtfsTransitConnection connection :
                firstConnections) {

            if (connection == null) {
                continue;
            }

            if (connection.departureTime() == null ||
                    connection.arrivalTime() == null) {
                continue;
            }

            if (connection.originStopId() == null ||
                    !connection.originStopId()
                            .equals(originStopId)) {
                continue;
            }

            List<GtfsJourneyLeg> legs =
                    List.of(
                            toLeg(connection)
                    );

            SearchState state =
                    new SearchState(
                            connection.destinationStopId(),
                            connection.destinationParentStationId(),
                            connection.departureTime(),
                            connection.arrivalTime(),
                            0,
                            legs
                    );

            queue.offer(state);
        }

        /*
         * Prevent repeatedly exploring the same state.
         *
         * Key:
         *
         *     stop + trip + transfer count
         */
        Set<String> visited =
                new HashSet<>();

        /*
         * Keep the best arrival discovered at each
         * stop/transfer combination.
         */
        Map<String, LocalDateTime> bestArrival =
                new HashMap<>();

        List<GtfsJourney> results =
                new ArrayList<>();

        while (!queue.isEmpty() &&
                results.size() < safeLimit * 4) {

            SearchState state =
                    queue.poll();

            if (state == null) {
                continue;
            }

            if (state.arrivalTime() == null) {
                continue;
            }

            GtfsJourneyLeg lastLeg =
                    state.lastLeg();

            if (lastLeg == null) {
                continue;
            }

            String stateKey =
                    buildStateKey(
                            state
                    );

            if (!visited.add(stateKey)) {
                continue;
            }

            String stopId =
                    state.currentStopId();

            if (stopId == null ||
                    stopId.isBlank()) {
                continue;
            }

            /*
             * Destination reached.
             */
            if (destinationStopId.equals(
                    stopId
            )) {

                GtfsJourney journey =
                        buildJourney(
                                feedId,
                                originStopId,
                                destinationStopId,
                                state
                        );

                if (journey != null) {

                    results.add(
                            journey
                    );
                }

                /*
                 * We continue searching because another
                 * journey may have a better arrival time.
                 */
                continue;
            }

            /*
             * Maximum number of transfers reached.
             *
             * A journey with N legs has N-1 transfers.
             */
            if (state.transferCount()
                    >= safeMaxTransfers) {

                continue;
            }

            /*
             * Find connections leaving the current stop.
             */
            List<GtfsTransitConnection> exactConnections =
                    graphRepository.findConnectionsFromStop(
                            feedId,
                            stopId,
                            serviceIds,
                            CONNECTION_BATCH_SIZE
                    );

            List<GtfsTransitConnection> stationConnections =
                    List.of();

            if (state.currentParentStationId() != null
                    && !state.currentParentStationId().isBlank()) {

                stationConnections =
                        graphRepository.findConnectionsFromParentStation(
                                feedId,
                                state.currentParentStationId(),
                                serviceIds,
                                CONNECTION_BATCH_SIZE
                        );
            }

            Map<String, GtfsTransitConnection> connectionMap =
                    new LinkedHashMap<>();

            for (GtfsTransitConnection connection : exactConnections) {
                connectionMap.putIfAbsent(
                        connectionKey(connection),
                        connection
                );
            }

            for (GtfsTransitConnection connection : stationConnections) {
                connectionMap.putIfAbsent(
                        connectionKey(connection),
                        connection
                );
            }

            List<GtfsTransitConnection> connections =
                    new ArrayList<>(connectionMap.values());

            connections.sort(
                    Comparator.comparing(
                            GtfsTransitConnection::departureTime,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    )
            );

            if (connections.isEmpty()) {
                continue;
            }

            for (GtfsTransitConnection connection :
                    connections) {

                if (connection == null) {
                    continue;
                }

                if (connection.departureTime() == null ||
                        connection.arrivalTime() == null) {
                    continue;
                }

                boolean sameTrip =
                        sameTripAsLastLeg(
                                connection,
                                lastLeg,
                                state.currentStopId()
                        );

                /*
                 * Continuing on the same trip from the exact current
                 * stop does not require a transfer buffer.
                 */
                if (sameTrip) {

                    if (connection.departureTime()
                            .isBefore(
                                    state.arrivalTime()
                            )) {
                        continue;
                    }

                } else {

                    /*
                     * Different trip:
                     *
                     * - same stop transfer
                     * - platform-to-platform transfer through parent_station
                     *
                     * Both use the configured minimum transfer time.
                     */
                    LocalDateTime earliestDeparture =
                            state.arrivalTime()
                                    .plusMinutes(
                                            safeTransferMinutes
                                    );

                    if (connection.departureTime()
                            .isBefore(
                                    earliestDeparture
                            )) {
                        continue;
                    }
                }

                /*
                 * Prevent immediately taking the exact same
                 * trip again. The current connection already
                 * represents that trip.
                 */
                if (sameTripAsLastLeg(
                        connection,
                        lastLeg,
                        state.currentStopId()
                )) {

                    /*
                     * Same-trip continuation is not a transfer.
                     *
                     * We still allow it because moving from one
                     * stop to another on the same trip is a normal
                     * continuation of the same leg.
                     */
                }

                /*
                 * Do not return to a stop already present in the
                 * current journey. This avoids loops such as:
                 *
                 * A -> B -> A -> B
                 */
                if (containsStop(
                        state.legs(),
                        connection.destinationStopId()
                )) {
                    continue;
                }

                /*
                 * Build next legs.
                 */
                List<GtfsJourneyLeg> nextLegs =
                        new ArrayList<>(
                                state.legs()
                        );

                /*
                 * If this connection belongs to the same trip
                 * as the previous leg, it is a continuation and
                 * should remain part of the same transit leg.
                 *
                 * Otherwise it represents a transfer.
                 */
                if (sameTripAsLastLeg(
                        connection,
                        lastLeg,
                        state.currentStopId()
                )) {

                    GtfsJourneyLeg mergedLeg =
                            extendLeg(
                                    lastLeg,
                                    connection
                            );

                    nextLegs.set(
                            nextLegs.size() - 1,
                            mergedLeg
                    );

                } else {

                    nextLegs.add(
                            toLeg(connection)
                    );
                }

                int nextTransfers =
                        countTransfers(
                                nextLegs
                        );

                if (nextTransfers >
                        safeMaxTransfers) {

                    continue;
                }

                SearchState nextState =
                        new SearchState(
                                connection.destinationStopId(),
                                connection.destinationParentStationId(),
                                state.departureTime(),
                                connection.arrivalTime(),
                                nextTransfers,
                                List.copyOf(
                                        nextLegs
                                )
                        );

                String nextKey =
                        buildStateKey(
                                nextState
                        );

                LocalDateTime previousBest =
                        bestArrival.get(
                                nextKey
                        );

                if (previousBest != null &&
                        !connection.arrivalTime()
                                .isBefore(
                                        previousBest
                                )) {

                    continue;
                }

                bestArrival.put(
                        nextKey,
                        connection.arrivalTime()
                );

                queue.offer(
                        nextState
                );
            }
        }

        return finalizeResults(
                results,
                safeLimit
        );
    }

    /**
     * Convenience overload using three transfers.
     */
    public List<GtfsJourney> findJourneys(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
    ) {

        return findJourneys(
                feedId,
                originStopId,
                destinationStopId,
                date,
                DEFAULT_MAX_TRANSFERS,
                DEFAULT_MIN_TRANSFER_MINUTES,
                20
        );
    }

    /**
     * Converts a graph connection into a journey leg.
     */
    private GtfsJourneyLeg toLeg(
            GtfsTransitConnection connection
    ) {

        String destination =
                connection.tripHeadsign();

        if (destination == null ||
                destination.isBlank()) {

            destination =
                    connection.destinationStopName();
        }

        return new GtfsJourneyLeg(
                connection.feedId(),

                connection.originStopId(),
                connection.originStopName(),

                connection.destinationStopId(),
                connection.destinationStopName(),

                connection.tripId(),

                connection.routeId(),
                connection.routeShortName(),
                connection.routeLongName(),
                connection.routeType(),

                connection.serviceId(),

                connection.departureTime(),
                connection.arrivalTime(),

                connection.travelDuration(),

                1,

                destination,

                "SCHEDULED"
        );
    }

    /**
     * Extends a leg when the next connection belongs to
     * the same trip.
     *
     * Example:
     *
     * A -> B
     * B -> C
     *
     * becomes:
     *
     * A -> C
     */
    private GtfsJourneyLeg extendLeg(
            GtfsJourneyLeg currentLeg,
            GtfsTransitConnection connection
    ) {

        LocalDateTime departure =
                currentLeg.departureTime();

        LocalDateTime arrival =
                connection.arrivalTime();

        Duration duration =
                Duration.ZERO;

        if (departure != null &&
                arrival != null) {

            long minutes =
                    Duration.between(
                            departure,
                            arrival
                    ).toMinutes();

            duration =
                    Duration.ofMinutes(
                            Math.max(
                                    0,
                                    minutes
                            )
                    );
        }

        String destination =
                connection.tripHeadsign();

        if (destination == null ||
                destination.isBlank()) {

            destination =
                    connection.destinationStopName();
        }

        return new GtfsJourneyLeg(
                currentLeg.feedId(),

                currentLeg.originStopId(),
                currentLeg.originStopName(),

                connection.destinationStopId(),
                connection.destinationStopName(),

                currentLeg.tripId(),

                currentLeg.routeId(),
                currentLeg.routeShortName(),
                currentLeg.routeLongName(),
                currentLeg.routeType(),

                currentLeg.serviceId(),

                departure,
                arrival,

                duration,

                connection.toStopSequence(),

                destination,

                "SCHEDULED"
        );
    }

    /**
     * Builds the final public journey object.
     */
    private GtfsJourney buildJourney(
            String feedId,
            String originStopId,
            String destinationStopId,
            SearchState state
    ) {

        List<GtfsJourneyLeg> legs =
                state.legs();

        if (legs == null ||
                legs.isEmpty()) {

            return null;
        }

        GtfsJourneyLeg firstLeg =
                legs.get(0);

        GtfsJourneyLeg lastLeg =
                legs.get(
                        legs.size() - 1
                );

        LocalDateTime departureTime =
                firstLeg.departureTime();

        LocalDateTime arrivalTime =
                lastLeg.arrivalTime();

        if (departureTime == null ||
                arrivalTime == null) {

            return null;
        }

        long totalMinutes =
                Duration.between(
                        departureTime,
                        arrivalTime
                ).toMinutes();

        if (totalMinutes < 0) {
            return null;
        }

        long travelMinutes =
                0;

        long waitingMinutes =
                0;

        for (int i = 0;
             i < legs.size();
             i++) {

            GtfsJourneyLeg leg =
                    legs.get(i);

            if (leg == null) {
                continue;
            }

            travelMinutes +=
                    leg.durationMinutes();

            if (i > 0) {

                GtfsJourneyLeg previous =
                        legs.get(
                                i - 1
                        );

                if (previous != null &&
                        previous.arrivalTime() != null &&
                        leg.departureTime() != null) {

                    long wait =
                            Duration.between(
                                    previous.arrivalTime(),
                                    leg.departureTime()
                            ).toMinutes();

                    waitingMinutes +=
                            Math.max(
                                    0,
                                    wait
                            );
                }
            }
        }

        long transferMinutes =
                waitingMinutes;

        int transfers =
                Math.max(
                        0,
                        legs.size() - 1
                );

        String destination =
                lastLeg.destination();

        if (destination == null ||
                destination.isBlank()) {

            destination =
                    lastLeg.destinationStopName();
        }

        Duration journeyDuration =
                Duration.ofMinutes(
                        totalMinutes
                );

        /*
         * The top-level trip/route fields retain the first
         * leg for backward compatibility.
         *
         * The complete multimodal journey is represented by
         * legs.
         */
        return new GtfsJourney(
                feedId,

                originStopId,
                firstLeg.originStopName(),

                destinationStopId,
                lastLeg.destinationStopName(),

                firstLeg.tripId(),

                firstLeg.routeId(),
                firstLeg.routeShortName(),
                firstLeg.routeLongName(),
                firstLeg.routeType(),

                firstLeg.serviceId(),

                departureTime,
                arrivalTime,

                journeyDuration,

                transfers,

                destination,

                "SCHEDULED",

                List.copyOf(
                        legs
                ),

                Duration.ofMinutes(
                        waitingMinutes
                ),

                Duration.ofMinutes(
                        transferMinutes
                )
        );
    }

    /**
     * Final sorting and duplicate removal.
     */
    private List<GtfsJourney> finalizeResults(
            List<GtfsJourney> journeys,
            int limit
    ) {

        if (journeys == null ||
                journeys.isEmpty()) {

            return List.of();
        }

        Map<String, GtfsJourney> unique =
                new LinkedHashMap<>();

        for (GtfsJourney journey :
                journeys) {

            if (journey == null) {
                continue;
            }

            unique.putIfAbsent(
                    journeyKey(journey),
                    journey
            );
        }

        List<GtfsJourney> sorted =
                new ArrayList<>(
                        unique.values()
                );

        sorted.sort(
                Comparator
                        .comparing(
                                GtfsJourney::arrivalTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                        .thenComparing(
                                GtfsJourney::departureTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                        .thenComparingInt(
                                GtfsJourney::transferCount
                        )
                        .thenComparingLong(
                                GtfsJourney::durationMinutes
                        )
        );

        if (sorted.size() <= limit) {
            return List.copyOf(
                    sorted
            );
        }

        return List.copyOf(
                sorted.subList(
                        0,
                        limit
                )
        );
    }

    /**
     * Builds a stable journey identity.
     */
    private String journeyKey(
            GtfsJourney journey
    ) {

        StringBuilder key =
                new StringBuilder();

        key.append(
                safe(
                        journey.feedId()
                )
        );

        key.append('|');

        key.append(
                safe(
                        journey.originStopId()
                )
        );

        key.append('|');

        key.append(
                safe(
                        journey.destinationStopId()
                )
        );

        key.append('|');

        key.append(
                journey.departureTime()
        );

        key.append('|');

        key.append(
                journey.arrivalTime()
        );

        key.append('|');

        if (journey.legs() != null) {

            for (GtfsJourneyLeg leg :
                    journey.legs()) {

                if (leg == null) {
                    continue;
                }

                key.append(
                        safe(
                                leg.tripId()
                        )
                );

                key.append(':');

                key.append(
                        safe(
                                leg.routeId()
                        )
                );

                key.append(':');

                key.append(
                        safe(
                                leg.originStopId()
                        )
                );

                key.append(':');

                key.append(
                        safe(
                                leg.destinationStopId()
                        )
                );

                key.append(';');
            }
        }

        return key.toString();
    }

    private String connectionKey(
            GtfsTransitConnection connection
    ) {

        if (connection == null) {
            return "";
        }

        return safe(connection.tripId())
                + "|"
                + safe(connection.originStopId())
                + "|"
                + safe(connection.destinationStopId())
                + "|"
                + String.valueOf(connection.departureTime())
                + "|"
                + String.valueOf(connection.arrivalTime());
    }

    private String buildStateKey(
            SearchState state
    ) {

        GtfsJourneyLeg lastLeg =
                state.lastLeg();

        String tripId =
                lastLeg == null
                        ? ""
                        : safe(
                                lastLeg.tripId()
                        );

        return safe(
                state.currentStopId()
        )
                + "|"
                + safe(
                        state.currentParentStationId()
                )
                + "|"
                + tripId
                + "|"
                + state.transferCount();
    }

    /**
     * Checks whether a connection continues the same trip.
     */
    private boolean sameTripAsLastLeg(
            GtfsTransitConnection connection,
            GtfsJourneyLeg lastLeg,
            String currentStopId
    ) {

        if (connection == null ||
                lastLeg == null) {

            return false;
        }

        if (connection.tripId() == null ||
                lastLeg.tripId() == null) {

            return false;
        }

        return connection.tripId()
                .equals(
                        lastLeg.tripId()
                )
                && connection.originStopId() != null
                && connection.originStopId().equals(
                        currentStopId
                );
    }

    /**
     * Prevents route loops.
     */
    private boolean containsStop(
            List<GtfsJourneyLeg> legs,
            String stopId
    ) {

        if (legs == null ||
                legs.isEmpty() ||
                stopId == null ||
                stopId.isBlank()) {

            return false;
        }

        for (GtfsJourneyLeg leg :
                legs) {

            if (leg == null) {
                continue;
            }

            if (stopId.equals(
                    leg.originStopId()
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * Number of actual route changes.
     */
    private int countTransfers(
            List<GtfsJourneyLeg> legs
    ) {

        if (legs == null ||
                legs.size() <= 1) {

            return 0;
        }

        int transfers = 0;

        for (int i = 1;
             i < legs.size();
             i++) {

            GtfsJourneyLeg previous =
                    legs.get(
                            i - 1
                    );

            GtfsJourneyLeg current =
                    legs.get(i);

            if (previous == null ||
                    current == null) {

                continue;
            }

            if (!safe(
                    previous.tripId()
            ).equals(
                    safe(
                            current.tripId()
                    )
            )) {

                transfers++;
            }
        }

        return transfers;
    }

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    private void validate(
            String feedId,
            String originStopId,
            String destinationStopId,
            LocalDate date
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

        if (date == null) {

            throw new IllegalArgumentException(
                    "Date is required"
            );
        }
    }

    /**
     * Internal search state.
     */
    private record SearchState(
            String currentStopId,
            String currentParentStationId,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            int transferCount,
            List<GtfsJourneyLeg> legs
    ) {

        GtfsJourneyLeg lastLeg() {

            if (legs == null ||
                    legs.isEmpty()) {

                return null;
            }

            return legs.get(
                    legs.size() - 1
            );
        }
    }
}