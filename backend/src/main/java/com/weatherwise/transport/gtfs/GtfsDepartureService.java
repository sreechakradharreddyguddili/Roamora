package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GtfsDepartureService {

    private final GtfsScheduleRepository scheduleRepository;

    private final GtfsServiceCalendarService calendarService;

    private final GtfsRealtimeService realtimeService;

    public GtfsDepartureService(
            GtfsScheduleRepository scheduleRepository,
            GtfsServiceCalendarService calendarService,
            GtfsRealtimeService realtimeService
    ) {

        this.scheduleRepository =
                scheduleRepository;

        this.calendarService =
                calendarService;

        this.realtimeService =
                realtimeService;
    }

    /**
     * Finds upcoming departures for a stop.
     *
     * Static GTFS is always the base schedule.
     *
     * If GTFS-Realtime data exists for the same:
     *
     *     feedId + tripId + stopId
     *
     * realtime estimated times and delays are applied.
     */
    public GtfsDepartureResponse findDepartures(
            String feedId,
            String stopId,
            LocalDate date,
            Integer limit
    ) {

        if (feedId == null
                || feedId.isBlank()) {

            throw new IllegalArgumentException(
                    "feedId is required."
            );
        }

        if (stopId == null
                || stopId.isBlank()) {

            throw new IllegalArgumentException(
                    "stopId is required."
            );
        }

        String normalizedFeedId =
                feedId.trim();

        String normalizedStopId =
                stopId.trim();

        int safeLimit =
                limit == null
                        ? 10
                        : Math.max(
                                1,
                                Math.min(
                                        limit,
                                        50
                                )
                        );

        /*
         * ------------------------------------------------------------
         * STOP TIMEZONE
         * ------------------------------------------------------------
         */

        String timezone =
                scheduleRepository.findStopTimezone(
                        normalizedFeedId,
                        normalizedStopId
                );

        ZoneId zoneId =
                resolveZoneId(timezone);

        /*
         * ------------------------------------------------------------
         * LOCAL SERVICE DATE
         * ------------------------------------------------------------
         *
         * The stop's timezone determines the current service date.
         */

        ZonedDateTime now =
                ZonedDateTime.now(zoneId);

        LocalDate requestedDate =
                date == null
                        ? now.toLocalDate()
                        : date;

        /*
         * ------------------------------------------------------------
         * ACTIVE SERVICES
         * ------------------------------------------------------------
         */

        Set<String> activeServices =
                calendarService.findActiveServices(
                        normalizedFeedId,
                        requestedDate
                );

        if (activeServices.isEmpty()) {

            return new GtfsDepartureResponse(
                    normalizedFeedId,
                    normalizedStopId,
                    null,
                    requestedDate,
                    now.toLocalTime(),
                    0,
                    List.of()
            );
        }

        /*
         * ------------------------------------------------------------
         * STATIC GTFS SCHEDULE
         * ------------------------------------------------------------
         */

        List<GtfsSchedule> schedules =
                scheduleRepository.findSchedules(
                        normalizedFeedId,
                        normalizedStopId,
                        requestedDate,
                        activeServices,
                        500
                );

        if (schedules.isEmpty()) {

            return new GtfsDepartureResponse(
                    normalizedFeedId,
                    normalizedStopId,
                    null,
                    requestedDate,
                    now.toLocalTime(),
                    0,
                    List.of()
            );
        }

        /*
         * The schedule repository also knows the stop timezone.
         *
         * Re-resolve it in case the first lookup did not contain
         * a usable timezone.
         */

        String scheduleTimezone =
                schedules.get(0).stopTimezone();

        if (scheduleTimezone != null
                && !scheduleTimezone.isBlank()) {

            zoneId =
                    resolveZoneId(
                            scheduleTimezone
                    );

            now =
                    ZonedDateTime.now(zoneId);
        }

        /*
         * ------------------------------------------------------------
         * GTFS REALTIME
         * ------------------------------------------------------------
         *
         * Load realtime updates once rather than querying the database
         * for every schedule row.
         */

        List<GtfsRealtimeTripUpdate> realtimeUpdates =
                realtimeService.findTripUpdates(
                        normalizedFeedId,
                        null,
                        normalizedStopId,
                        500
                );

        Map<String, GtfsRealtimeTripUpdate> realtimeByTrip =
                indexRealtimeUpdates(
                        realtimeUpdates
                );

        /*
         * ------------------------------------------------------------
         * BUILD DEPARTURES
         * ------------------------------------------------------------
         */

        List<GtfsDeparture> result =
                new ArrayList<>();

        for (GtfsSchedule schedule :
                schedules) {

            if (schedule == null) {
                continue;
            }

            if (schedule.tripId() == null
                    || schedule.tripId().isBlank()) {
                continue;
            }

            String staticDeparture =
                    schedule.departureTime();

            if (staticDeparture == null
                    || staticDeparture.isBlank()) {
                continue;
            }

            /*
             * Find realtime update for this exact trip and stop.
             */

            GtfsRealtimeTripUpdate realtime =
                    realtimeByTrip.get(
                            schedule.tripId()
                    );

            DepartureTiming timing =
                    resolveDepartureTiming(
                            schedule,
                            realtime,
                            requestedDate,
                            now
                    );

            if (timing == null) {
                continue;
            }

            /*
             * Do not return departures that have already passed.
             */

            if (timing.secondsUntilDeparture() < 0) {
                continue;
            }

            long minutesUntil =
                    (timing.secondsUntilDeparture() + 59)
                            / 60;

            String destination =
                    schedule.tripHeadsign();

            if (destination == null
                    || destination.isBlank()) {

                destination =
                        schedule.stopHeadsign();
            }

            String status =
                    resolveDepartureStatus(
                            realtime
                    );

            Integer delaySeconds =
                    resolveDelaySeconds(
                            realtime,
                            schedule,
                            timing
                    );

            /*
             * If realtime says this departure is cancelled,
             * keep it visible instead of silently treating it
             * as a normal scheduled departure.
             */

            result.add(
                    new GtfsDeparture(
                            schedule.stopId(),
                            schedule.stopName(),
                            schedule.tripId(),
                            schedule.routeId(),
                            schedule.routeShortName(),
                            schedule.routeLongName(),
                            destination,
                            schedule.serviceId(),
                            schedule.arrivalTime(),
                            schedule.departureTime(),
                            timing.estimatedArrival(),
                            timing.estimatedDeparture(),
                            delaySeconds,
                            minutesUntil,
                            status,
                            realtime != null
                    )
            );

            /*
             * Keep the requested number of useful departures.
             */

            if (result.size() >= safeLimit) {
                break;
            }
        }

        /*
         * ------------------------------------------------------------
         * RESPONSE
         * ------------------------------------------------------------
         */

        return new GtfsDepartureResponse(
                normalizedFeedId,
                normalizedStopId,
                schedules.get(0).stopName(),
                requestedDate,
                now.toLocalTime(),
                result.size(),
                result
        );
    }

    /**
     * Creates an index using trip ID.
     *
     * The realtime query is already restricted to the requested stop,
     * therefore tripId is sufficient here.
     */
    private Map<String, GtfsRealtimeTripUpdate> indexRealtimeUpdates(
            List<GtfsRealtimeTripUpdate> updates
    ) {

        Map<String, GtfsRealtimeTripUpdate> result =
                new HashMap<>();

        if (updates == null
                || updates.isEmpty()) {

            return result;
        }

        for (GtfsRealtimeTripUpdate update :
                updates) {

            if (update == null) {
                continue;
            }

            String tripId =
                    update.tripId();

            if (tripId == null
                    || tripId.isBlank()) {
                continue;
            }

            /*
             * If several updates exist for the same trip/stop,
             * prefer the newest fetched update.
             */

            GtfsRealtimeTripUpdate existing =
                    result.get(tripId);

            if (existing == null) {

                result.put(
                        tripId,
                        update
                );

                continue;
            }

            if (isNewer(
                    update,
                    existing
            )) {

                result.put(
                        tripId,
                        update
                );
            }
        }

        return result;
    }

    /**
     * Determines whether the realtime update is newer.
     */
    private boolean isNewer(
            GtfsRealtimeTripUpdate candidate,
            GtfsRealtimeTripUpdate existing
    ) {

        if (candidate.fetchedAt() == null) {
            return false;
        }

        if (existing.fetchedAt() == null) {
            return true;
        }

        return candidate.fetchedAt()
                .isAfter(existing.fetchedAt());
    }

    /**
     * Resolves scheduled/realtime departure timing.
     */
    private DepartureTiming resolveDepartureTiming(
            GtfsSchedule schedule,
            GtfsRealtimeTripUpdate realtime,
            LocalDate serviceDate,
            ZonedDateTime now
    ) {

        if (schedule == null
                || schedule.departureTime() == null
                || schedule.departureTime().isBlank()) {

            return null;
        }

        /*
         * Static GTFS departure.
         */

        ZonedDateTime scheduledDeparture =
                parseGtfsDateTime(
                        serviceDate,
                        schedule.departureTime(),
                        now.getZone()
                );

        if (scheduledDeparture == null) {
            return null;
        }

        /*
         * Static GTFS arrival.
         */

        ZonedDateTime scheduledArrival =
                parseGtfsDateTime(
                        serviceDate,
                        schedule.arrivalTime(),
                        now.getZone()
                );

        /*
         * No realtime update.
         *
         * Use the static schedule.
         */

        if (realtime == null) {

            long seconds =
                    Duration.between(
                            now,
                            scheduledDeparture
                    ).getSeconds();

            return new DepartureTiming(
                    scheduledArrival == null
                            ? null
                            : formatDateTime(
                                    scheduledArrival
                            ),
                    formatDateTime(
                            scheduledDeparture
                    ),
                    seconds
            );
        }

        /*
         * Realtime estimated departure.
         *
         * GTFS-RT estimated_time is stored as UTC.
         */

        ZonedDateTime estimatedDeparture =
                toZone(
                        realtime.estimatedTime(),
                        now.getZone()
                );

        /*
         * If RT provides only delay, derive the estimate
         * from the static GTFS departure.
         */

        if (estimatedDeparture == null
                && realtime.delaySeconds() != null) {

            estimatedDeparture =
                    scheduledDeparture.plusSeconds(
                            realtime.delaySeconds()
                    );
        }

        /*
         * If RT has no usable estimated time,
         * fall back to the static schedule.
         */

        if (estimatedDeparture == null) {

            estimatedDeparture =
                    scheduledDeparture;
        }

        /*
         * Realtime does not currently store a separate
         * estimated arrival in our database model.
         *
         * Therefore preserve the static scheduled arrival.
         */

        String estimatedArrival =
                scheduledArrival == null
                        ? null
                        : formatDateTime(
                                scheduledArrival
                        );

        long seconds =
                Duration.between(
                        now,
                        estimatedDeparture
                ).getSeconds();

        return new DepartureTiming(
                estimatedArrival,
                formatDateTime(
                        estimatedDeparture
                ),
                seconds
        );
    }

    /**
     * Determines the public departure status.
     */
    private String resolveDepartureStatus(
            GtfsRealtimeTripUpdate realtime
    ) {

        if (realtime == null) {
            return "SCHEDULED";
        }

        String status =
                realtime.status();

        if (status == null
                || status.isBlank()) {

            if (realtime.delaySeconds() != null
                    && realtime.delaySeconds() != 0) {

                return "DELAYED";
            }

            return "SCHEDULED";
        }

        String normalized =
                status.trim()
                        .toUpperCase();

        if ("CANCELED".equals(normalized)
                || "CANCELLED".equals(normalized)) {

            return "CANCELLED";
        }

        if ("SKIPPED".equals(normalized)) {
            return "SKIPPED";
        }

        if ("NO_DATA".equals(normalized)) {
            return "NO_DATA";
        }

        if ("DELAYED".equals(normalized)) {
            return "DELAYED";
        }

        if ("ADDED".equals(normalized)) {
            return "ADDED";
        }

        if ("DUPLICATED".equals(normalized)) {
            return "DUPLICATED";
        }

        if (realtime.delaySeconds() != null
                && realtime.delaySeconds() != 0) {

            return "DELAYED";
        }

        return "SCHEDULED";
    }

    /**
     * Gets delay from realtime.
     *
     * If realtime did not explicitly provide delay but provided
     * an estimated departure, calculate the difference from the
     * static GTFS schedule.
     */
    private Integer resolveDelaySeconds(
            GtfsRealtimeTripUpdate realtime,
            GtfsSchedule schedule,
            DepartureTiming timing
    ) {

        if (realtime == null) {
            return null;
        }

        if (realtime.delaySeconds() != null) {
            return realtime.delaySeconds();
        }

        if (schedule == null
                || schedule.departureTime() == null
                || timing == null
                || timing.estimatedDeparture() == null) {

            return null;
        }

        try {

            LocalTime scheduledTime =
                    parseGtfsLocalTime(
                            schedule.departureTime()
                    );

            LocalDateTime estimated =
                    LocalDateTime.parse(
                            timing.estimatedDeparture()
                    );

            LocalDateTime scheduled =
                    LocalDateTime.of(
                            estimated.toLocalDate(),
                            scheduledTime
                    );

            long seconds =
                    Duration.between(
                            scheduled,
                            estimated
                    ).getSeconds();

            if (seconds > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }

            if (seconds < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }

            return (int) seconds;

        } catch (Exception ignored) {

            return null;
        }
    }

    /**
     * Converts GTFS realtime UTC timestamp into the stop timezone.
     */
    private ZonedDateTime toZone(
            OffsetDateTime value,
            ZoneId zoneId
    ) {

        if (value == null) {
            return null;
        }

        return value
                .toInstant()
                .atZone(zoneId);
    }

    /**
     * Parses GTFS HH:mm:ss.
     *
     * GTFS allows values such as:
     *
     *     24:30:00
     *     25:15:00
     *
     * which belong to the following calendar date.
     */
    private ZonedDateTime parseGtfsDateTime(
            LocalDate serviceDate,
            String gtfsTime,
            ZoneId zoneId
    ) {

        if (serviceDate == null
                || gtfsTime == null
                || gtfsTime.isBlank()) {

            return null;
        }

        String[] parts =
                gtfsTime
                        .trim()
                        .split(":");

        if (parts.length != 3) {
            return null;
        }

        try {

            int hour =
                    Integer.parseInt(
                            parts[0]
                    );

            int minute =
                    Integer.parseInt(
                            parts[1]
                    );

            int second =
                    Integer.parseInt(
                            parts[2]
                    );

            if (hour < 0
                    || minute < 0
                    || minute > 59
                    || second < 0
                    || second > 59) {

                return null;
            }

            long totalSeconds =
                    hour * 3600L
                            + minute * 60L
                            + second;

            long daysAfterServiceDate =
                    Math.floorDiv(
                            totalSeconds,
                            86_400L
                    );

            long secondsOfDay =
                    Math.floorMod(
                            totalSeconds,
                            86_400L
                    );

            LocalDate actualDate =
                    serviceDate.plusDays(
                            daysAfterServiceDate
                    );

            LocalTime time =
                    LocalTime.ofSecondOfDay(
                            secondsOfDay
                    );

            return ZonedDateTime.of(
                    actualDate,
                    time,
                    zoneId
            );

        } catch (
                NumberFormatException
                        | DateTimeException exception
        ) {

            return null;
        }
    }

    /**
     * Parses only the local clock portion of a GTFS time.
     */
    private LocalTime parseGtfsLocalTime(
            String gtfsTime
    ) {

        String[] parts =
                gtfsTime
                        .trim()
                        .split(":");

        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid GTFS time: " + gtfsTime
            );
        }

        int hour =
                Integer.parseInt(parts[0]);

        int minute =
                Integer.parseInt(parts[1]);

        int second =
                Integer.parseInt(parts[2]);

        long seconds =
                Math.floorMod(
                        hour * 3600L
                                + minute * 60L
                                + second,
                        86_400L
                );

        return LocalTime.ofSecondOfDay(
                seconds
        );
    }

    /**
     * Formats an estimated timestamp for the API.
     *
     * Local date/time is intentionally returned because the
     * departure API already operates in the stop's timezone.
     */
    private String formatDateTime(
            ZonedDateTime value
    ) {

        if (value == null) {
            return null;
        }

        return value
                .toLocalDateTime()
                .toString();
    }

    /**
     * Resolves an IANA timezone safely.
     */
    private ZoneId resolveZoneId(
            String timezone
    ) {

        String normalizedTimezone =
                timezone == null
                        || timezone.isBlank()
                        ? "UTC"
                        : timezone.trim();

        try {

            return ZoneId.of(
                    normalizedTimezone
            );

        } catch (DateTimeException exception) {

            return ZoneId.of("UTC");
        }
    }

    /**
     * Internal timing result.
     */
    private record DepartureTiming(

            String estimatedArrival,

            String estimatedDeparture,

            long secondsUntilDeparture
    ) {
    }
}