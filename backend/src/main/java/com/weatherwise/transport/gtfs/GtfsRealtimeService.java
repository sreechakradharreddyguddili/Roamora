package com.weatherwise.transport.gtfs;

import com.google.transit.realtime.GtfsRealtime;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class GtfsRealtimeService {

    private final GtfsRealtimeRepository repository;
    private final RestClient restClient;

    public GtfsRealtimeService(
            GtfsRealtimeRepository repository
    ) {
        this.repository = repository;
        this.restClient = RestClient.builder()
                .build();
    }

    @Transactional
    public GtfsRealtimeRefreshResult refreshFeed(GtfsFeed feed) {

        if (feed == null
                || feed.getFeedId() == null
                || feed.getFeedId().isBlank()) {
            throw new IllegalArgumentException("GTFS feed is required");
        }

        if (!feed.isActive()) {
            throw new IllegalArgumentException(
                    "GTFS feed is inactive: " + feed.getFeedId()
            );
        }

        if (!feed.isRealtimeEnabled()) {
            throw new IllegalArgumentException(
                    "GTFS realtime is disabled for feed: " + feed.getFeedId()
            );
        }

        String feedId = feed.getFeedId();
        OffsetDateTime fetchedAt = OffsetDateTime.now();

        List<GtfsRealtimeTripUpdate> tripUpdates =
                feed.getRealtimeTripUpdatesUrl() == null
                        || feed.getRealtimeTripUpdatesUrl().isBlank()
                        ? List.of()
                        : fetchTripUpdates(
                                feed.getRealtimeTripUpdatesUrl(),
                                feedId,
                                fetchedAt
                        );

        List<GtfsRealtimeVehicle> vehicles =
                feed.getRealtimeVehiclePositionsUrl() == null
                        || feed.getRealtimeVehiclePositionsUrl().isBlank()
                        ? List.of()
                        : fetchVehicles(
                                feed.getRealtimeVehiclePositionsUrl(),
                                feedId,
                                fetchedAt
                        );

        List<GtfsRealtimeAlert> alerts =
                feed.getRealtimeAlertsUrl() == null
                        || feed.getRealtimeAlertsUrl().isBlank()
                        ? List.of()
                        : fetchAlerts(
                                feed.getRealtimeAlertsUrl(),
                                feedId,
                                fetchedAt
                        );

        repository.replaceTripUpdates(feedId, tripUpdates);
        repository.replaceVehicles(feedId, vehicles);
        repository.replaceAlerts(feedId, alerts);

        return new GtfsRealtimeRefreshResult(
                feedId,
                fetchedAt,
                tripUpdates.size(),
                vehicles.size(),
                alerts.size()
        );
    }

    public List<GtfsRealtimeTripUpdate> findTripUpdates(
            String feedId,
            String tripId,
            String stopId,
            int limit
    ) {
        return repository.findTripUpdates(
                feedId,
                tripId,
                stopId,
                limit
        );
    }

    public List<GtfsRealtimeVehicle> findVehicles(
            String feedId,
            String tripId,
            int limit
    ) {
        return repository.findVehicles(
                feedId,
                tripId,
                limit
        );
    }

    public List<GtfsRealtimeAlert> findAlerts(
            String feedId,
            int limit
    ) {
        return repository.findAlerts(
                feedId,
                limit
        );
    }

    private List<GtfsRealtimeTripUpdate> fetchTripUpdates(
            String url,
            String feedId,
            OffsetDateTime fetchedAt
    ) {
        GtfsRealtime.FeedMessage message = fetch(url);

        List<GtfsRealtimeTripUpdate> result = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {

            if (!entity.hasTripUpdate()) {
                continue;
            }

            GtfsRealtime.TripUpdate tripUpdate =
                    entity.getTripUpdate();

            String tripId = tripUpdate.hasTrip()
                    ? tripUpdate.getTrip().getTripId()
                    : null;

            String routeId = tripUpdate.hasTrip()
                    && tripUpdate.getTrip().hasRouteId()
                    ? tripUpdate.getTrip().getRouteId()
                    : null;

            String tripStatus = resolveTripStatus(tripUpdate);

            for (GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate
                    : tripUpdate.getStopTimeUpdateList()) {

                String stopId = stopTimeUpdate.hasStopId()
                        ? stopTimeUpdate.getStopId()
                        : null;

                Integer stopSequence =
                        stopTimeUpdate.hasStopSequence()
                                ? stopTimeUpdate.getStopSequence()
                                : null;

                TimePair times = resolveTimes(stopTimeUpdate);

                Integer delaySeconds =
                        resolveDelay(stopTimeUpdate);

                String status = resolveStopStatus(
                        tripStatus,
                        stopTimeUpdate
                );

                result.add(
                        new GtfsRealtimeTripUpdate(
                                null,
                                feedId,
                                tripId,
                                routeId,
                                stopId,
                                stopSequence,
                                times.scheduledTime(),
                                times.estimatedTime(),
                                delaySeconds,
                                status,
                                fetchedAt
                        )
                );
            }
        }

        return result;
    }

    private List<GtfsRealtimeVehicle> fetchVehicles(
            String url,
            String feedId,
            OffsetDateTime fetchedAt
    ) {
        GtfsRealtime.FeedMessage message = fetch(url);

        List<GtfsRealtimeVehicle> result = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {

            if (!entity.hasVehicle()) {
                continue;
            }

            GtfsRealtime.VehiclePosition vehicle =
                    entity.getVehicle();

            String vehicleId = null;
            String tripId = null;
            String routeId = null;

            if (vehicle.hasVehicle()) {
                vehicleId = vehicle.getVehicle().getId();

                if (vehicle.getVehicle().hasLabel()
                        && (vehicleId == null || vehicleId.isBlank())) {
                    vehicleId = vehicle.getVehicle().getLabel();
                }
            }

            if (vehicle.hasTrip()) {
                if (vehicle.getTrip().hasTripId()) {
                    tripId = vehicle.getTrip().getTripId();
                }

                if (vehicle.getTrip().hasRouteId()) {
                    routeId = vehicle.getTrip().getRouteId();
                }
            }

            Double latitude = null;
            Double longitude = null;
            Double bearing = null;
            Double speed = null;

            if (vehicle.hasPosition()) {
                GtfsRealtime.Position position =
                        vehicle.getPosition();

                if (position.hasLatitude()) {
                    latitude = (double) position.getLatitude();
                }

                if (position.hasLongitude()) {
                    longitude = (double) position.getLongitude();
                }

                if (position.hasBearing()) {
                    bearing = (double) position.getBearing();
                }

                if (position.hasSpeed()) {
                    speed = (double) position.getSpeed();
                }
            }

            Integer currentStopSequence =
                    vehicle.hasCurrentStopSequence()
                            ? vehicle.getCurrentStopSequence()
                            : null;

            String currentStatus =
                    vehicle.hasCurrentStatus()
                            ? vehicle.getCurrentStatus().name()
                            : null;

            result.add(
                    new GtfsRealtimeVehicle(
                            null,
                            feedId,
                            vehicleId,
                            tripId,
                            routeId,
                            latitude,
                            longitude,
                            bearing,
                            speed,
                            currentStopSequence,
                            currentStatus,
                            fetchedAt
                    )
            );
        }

        return result;
    }

    private List<GtfsRealtimeAlert> fetchAlerts(
            String url,
            String feedId,
            OffsetDateTime fetchedAt
    ) {
        GtfsRealtime.FeedMessage message = fetch(url);

        List<GtfsRealtimeAlert> result = new ArrayList<>();

        for (GtfsRealtime.FeedEntity entity : message.getEntityList()) {

            if (!entity.hasAlert()) {
                continue;
            }

            GtfsRealtime.Alert alert =
                    entity.getAlert();

            String alertId = entity.hasId()
                    ? entity.getId()
                    : null;

            String cause = alert.hasCause()
                    ? alert.getCause().name()
                    : null;

            String effect = alert.hasEffect()
                    ? alert.getEffect().name()
                    : null;

            String headerText =
                    extractTranslatedText(
                            alert.hasHeaderText()
                                    ? alert.getHeaderText()
                                    : null
                    );

            String descriptionText =
                    extractTranslatedText(
                            alert.hasDescriptionText()
                                    ? alert.getDescriptionText()
                                    : null
                    );

            String urlText =
                    extractTranslatedText(
                            alert.hasUrl()
                                    ? alert.getUrl()
                                    : null
                    );

            OffsetDateTime activeFrom = null;
            OffsetDateTime activeUntil = null;

            if (!alert.getActivePeriodList().isEmpty()) {
                GtfsRealtime.TimeRange range =
                        alert.getActivePeriod(0);

                if (range.hasStart()) {
                    activeFrom = Instant.ofEpochSecond(
                            range.getStart()
                    ).atOffset(ZoneOffset.UTC);
                }

                if (range.hasEnd()) {
                    activeUntil = Instant.ofEpochSecond(
                            range.getEnd()
                    ).atOffset(ZoneOffset.UTC);
                }
            }

            result.add(
                    new GtfsRealtimeAlert(
                            null,
                            feedId,
                            alertId,
                            cause,
                            effect,
                            headerText,
                            descriptionText,
                            urlText,
                            activeFrom,
                            activeUntil,
                            fetchedAt
                    )
            );
        }

        return result;
    }

    private GtfsRealtime.FeedMessage fetch(String url) {

        try {
            byte[] bytes = restClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .body(byte[].class);

            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException(
                        "Empty GTFS realtime response: " + url
                );
            }

            return GtfsRealtime.FeedMessage.parseFrom(
                    new ByteArrayInputStream(bytes)
            );

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to parse GTFS realtime feed: " + url,
                    ex
            );
        }
    }

    private TimePair resolveTimes(
            GtfsRealtime.TripUpdate.StopTimeUpdate update
    ) {
        OffsetDateTime scheduled = null;
        OffsetDateTime estimated = null;

        if (update.hasArrival()) {
            GtfsRealtime.TripUpdate.StopTimeEvent arrival =
                    update.getArrival();

            if (arrival.hasTime()) {
                estimated = toUtc(arrival.getTime());
            }

            if (arrival.hasDelay()) {
                // The static scheduled time is supplied by GTFS.
                // RT gives the delay/estimated event, not necessarily
                // a second independent scheduled timestamp.
            }
        }

        if (update.hasDeparture()) {
            GtfsRealtime.TripUpdate.StopTimeEvent departure =
                    update.getDeparture();

            if (departure.hasTime()) {
                estimated = toUtc(departure.getTime());
            }
        }

        return new TimePair(scheduled, estimated);
    }

    private Integer resolveDelay(
            GtfsRealtime.TripUpdate.StopTimeUpdate update
    ) {
        if (update.hasArrival()
                && update.getArrival().hasDelay()) {
            return update.getArrival().getDelay();
        }

        if (update.hasDeparture()
                && update.getDeparture().hasDelay()) {
            return update.getDeparture().getDelay();
        }

        return null;
    }

    private String resolveTripStatus(
            GtfsRealtime.TripUpdate update
    ) {
        if (!update.hasTrip()
                || !update.getTrip().hasScheduleRelationship()) {
            return "SCHEDULED";
        }

        return update.getTrip()
                .getScheduleRelationship()
                .name();
    }

    private String resolveStopStatus(
            String tripStatus,
            GtfsRealtime.TripUpdate.StopTimeUpdate update
    ) {
        if ("CANCELED".equalsIgnoreCase(tripStatus)
                || "CANCELLED".equalsIgnoreCase(tripStatus)) {
            return "CANCELLED";
        }

        if (update.hasScheduleRelationship()) {
            String relationship =
                    update.getScheduleRelationship().name();

            if ("SKIPPED".equalsIgnoreCase(relationship)) {
                return "SKIPPED";
            }

            if ("NO_DATA".equalsIgnoreCase(relationship)) {
                return "NO_DATA";
            }
        }

        if (resolveDelay(update) != null) {
            return "DELAYED";
        }

        return "SCHEDULED";
    }

    private OffsetDateTime toUtc(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atOffset(ZoneOffset.UTC);
    }

    private String extractTranslatedText(
            GtfsRealtime.TranslatedString translated
    ) {
        if (translated == null
                || translated.getTranslationList().isEmpty()) {
            return null;
        }

        GtfsRealtime.TranslatedString.Translation first =
                translated.getTranslation(0);

        if (first.hasText()) {
            return first.getText();
        }

        return null;
    }

    private record TimePair(
            OffsetDateTime scheduledTime,
            OffsetDateTime estimatedTime
    ) {
    }
}
