package com.weatherwise.transport.gtfs;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GtfsJourneyRealtimeEnricher {

    private final GtfsRealtimeService realtimeService;

    public GtfsJourneyRealtimeEnricher(GtfsRealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    public List<GtfsJourney> enrich(List<GtfsJourney> journeys) {
        if (journeys == null || journeys.isEmpty()) {
            return List.of();
        }

        List<GtfsJourney> result = new ArrayList<>(journeys.size());

        for (GtfsJourney journey : journeys) {
            if (journey != null) {
                result.add(enrichJourney(journey));
            }
        }

        return List.copyOf(result);
    }

    public GtfsJourney enrichJourney(GtfsJourney journey) {
        if (journey == null) {
            return null;
        }

        List<GtfsJourneyLeg> originalLegs = journey.getLegs();

        if (originalLegs.isEmpty()) {
            return journey;
        }

        List<GtfsJourneyLeg> enrichedLegs = new ArrayList<>(originalLegs.size());

        boolean realtimeAvailable = false;
        Integer maximumDelay = null;
        String journeyStatus = "SCHEDULED";
        OffsetDateTime estimatedDeparture = null;
        OffsetDateTime estimatedArrival = null;

        List<GtfsRealtimeVehicle> vehicles = new ArrayList<>();
        List<GtfsRealtimeAlert> alerts = new ArrayList<>();

        for (int index = 0; index < originalLegs.size(); index++) {
            GtfsJourneyLeg leg = originalLegs.get(index);

            if (leg == null) {
                continue;
            }

            GtfsJourneyLeg enrichedLeg = enrichLeg(leg);
            enrichedLegs.add(enrichedLeg);

            if (enrichedLeg.hasRealtime()) {
                realtimeAvailable = true;
            }

            if (enrichedLeg.delaySeconds() != null
                    && (maximumDelay == null
                    || enrichedLeg.delaySeconds() > maximumDelay)) {
                maximumDelay = enrichedLeg.delaySeconds();
            }

            journeyStatus = mergeStatus(journeyStatus, enrichedLeg.getStatus());

            if (index == 0) {
                estimatedDeparture = enrichedLeg.estimatedDepartureTime();
            }

            if (index == originalLegs.size() - 1) {
                estimatedArrival = enrichedLeg.estimatedArrivalTime();
            }

            addVehiclesForLeg(vehicles, enrichedLeg);
            addAlertsForFeed(alerts, enrichedLeg.feedId());
        }

        if (!vehicles.isEmpty() || !alerts.isEmpty()) {
            realtimeAvailable = true;
        }

        if (!realtimeAvailable) {
            return journey;
        }

        return new GtfsJourney(
                journey.feedId(),
                journey.originStopId(),
                journey.originStopName(),
                journey.destinationStopId(),
                journey.destinationStopName(),
                journey.tripId(),
                journey.routeId(),
                journey.routeShortName(),
                journey.routeLongName(),
                journey.routeType(),
                journey.serviceId(),
                journey.departureTime(),
                journey.arrivalTime(),
                journey.journeyDuration(),
                journey.transfers(),
                journey.destination(),
                journeyStatus,
                List.copyOf(enrichedLegs),
                journey.waitingTime(),
                journey.transferTime(),
                estimatedDeparture,
                estimatedArrival,
                maximumDelay,
                true,
                List.copyOf(vehicles),
                List.copyOf(alerts)
        );
    }

    private GtfsJourneyLeg enrichLeg(GtfsJourneyLeg leg) {
        if (leg.tripId() == null
                || leg.tripId().isBlank()
                || leg.feedId() == null
                || leg.feedId().isBlank()) {
            return leg;
        }

        GtfsRealtimeTripUpdate originUpdate = findBestUpdate(
                leg.feedId(), leg.tripId(), leg.originStopId());

        GtfsRealtimeTripUpdate destinationUpdate = findBestUpdate(
                leg.feedId(), leg.tripId(), leg.destinationStopId());

        if (originUpdate == null && destinationUpdate == null) {
            return leg;
        }

        OffsetDateTime estimatedDeparture =
                originUpdate == null ? null : originUpdate.estimatedTime();

        OffsetDateTime estimatedArrival =
                destinationUpdate == null ? null : destinationUpdate.estimatedTime();

        Integer delaySeconds =
                originUpdate != null && originUpdate.delaySeconds() != null
                        ? originUpdate.delaySeconds()
                        : destinationUpdate == null
                        ? null
                        : destinationUpdate.delaySeconds();

        String status = mergeStatus(
                "SCHEDULED",
                originUpdate == null ? null : originUpdate.status());

        status = mergeStatus(
                status,
                destinationUpdate == null ? null : destinationUpdate.status());

        return new GtfsJourneyLeg(
                leg.feedId(),
                leg.originStopId(),
                leg.originStopName(),
                leg.destinationStopId(),
                leg.destinationStopName(),
                leg.tripId(),
                leg.routeId(),
                leg.routeShortName(),
                leg.routeLongName(),
                leg.routeType(),
                leg.serviceId(),
                leg.departureTime(),
                leg.arrivalTime(),
                leg.travelDuration(),
                leg.stopCount(),
                leg.destination(),
                status,
                estimatedDeparture,
                estimatedArrival,
                delaySeconds,
                true
        );
    }

    private void addVehiclesForLeg(
            List<GtfsRealtimeVehicle> vehicles,
            GtfsJourneyLeg leg
    ) {
        if (leg.feedId() == null
                || leg.feedId().isBlank()
                || leg.tripId() == null
                || leg.tripId().isBlank()) {
            return;
        }

        List<GtfsRealtimeVehicle> found =
                realtimeService.findVehicles(leg.feedId(), leg.tripId(), 20);

        if (found == null) {
            return;
        }

        for (GtfsRealtimeVehicle vehicle : found) {
            if (vehicle == null) {
                continue;
            }

            if (vehicle.vehicleId() == null
                    && vehicle.tripId() == null) {
                continue;
            }

            boolean duplicate = vehicles.stream().anyMatch(existing ->
                    sameVehicle(existing, vehicle));

            if (!duplicate) {
                vehicles.add(vehicle);
            }
        }
    }

    private void addAlertsForFeed(
            List<GtfsRealtimeAlert> alerts,
            String feedId
    ) {
        if (feedId == null || feedId.isBlank()) {
            return;
        }

        List<GtfsRealtimeAlert> found =
                realtimeService.findAlerts(feedId, 100);

        if (found == null) {
            return;
        }

        for (GtfsRealtimeAlert alert : found) {
            if (alert == null) {
                continue;
            }

            boolean duplicate = alerts.stream().anyMatch(existing ->
                    sameAlert(existing, alert));

            if (!duplicate) {
                alerts.add(alert);
            }
        }
    }

    private boolean sameVehicle(
            GtfsRealtimeVehicle first,
            GtfsRealtimeVehicle second
    ) {
        if (first == second) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        if (first.id() != null && second.id() != null) {
            return first.id().equals(second.id());
        }

        if (first.vehicleId() != null && second.vehicleId() != null) {
            return first.vehicleId().equals(second.vehicleId())
                    && safeEquals(first.feedId(), second.feedId());
        }

        return first.tripId() != null
                && first.tripId().equals(second.tripId())
                && safeEquals(first.feedId(), second.feedId());
    }

    private boolean sameAlert(
            GtfsRealtimeAlert first,
            GtfsRealtimeAlert second
    ) {
        if (first == second) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        if (first.id() != null && second.id() != null) {
            return first.id().equals(second.id());
        }

        if (first.alertId() != null && second.alertId() != null) {
            return first.alertId().equals(second.alertId())
                    && safeEquals(first.feedId(), second.feedId());
        }

        return safeEquals(first.feedId(), second.feedId())
                && safeEquals(first.headerText(), second.headerText())
                && safeEquals(first.effect(), second.effect());
    }

    private boolean safeEquals(String first, String second) {
        return first == null
                ? second == null
                : first.equals(second);
    }

    private GtfsRealtimeTripUpdate findBestUpdate(
            String feedId,
            String tripId,
            String stopId
    ) {
        if (stopId == null || stopId.isBlank()) {
            return null;
        }

        List<GtfsRealtimeTripUpdate> updates =
                realtimeService.findTripUpdates(feedId, tripId, stopId, 20);

        if (updates == null || updates.isEmpty()) {
            return null;
        }

        GtfsRealtimeTripUpdate best = null;

        for (GtfsRealtimeTripUpdate update : updates) {
            if (update == null) {
                continue;
            }

            if (best == null) {
                best = update;
                continue;
            }

            OffsetDateTime candidateFetched = update.fetchedAt();
            OffsetDateTime bestFetched = best.fetchedAt();

            if (candidateFetched != null
                    && (bestFetched == null
                    || candidateFetched.isAfter(bestFetched))) {
                best = update;
            }
        }

        return best;
    }

    private String mergeStatus(String current, String incoming) {
        String normalizedCurrent = normalizeStatus(current);
        String normalizedIncoming = normalizeStatus(incoming);

        return statusPriority(normalizedIncoming)
                > statusPriority(normalizedCurrent)
                ? normalizedIncoming
                : normalizedCurrent;
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "CANCELLED", "CANCELED" -> 100;
            case "SKIPPED" -> 90;
            case "DELAYED" -> 80;
            case "NO_DATA" -> 50;
            case "ADDED" -> 40;
            case "DUPLICATED" -> 30;
            case "SCHEDULED" -> 10;
            default -> 20;
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SCHEDULED";
        }

        String normalized = status.trim().toUpperCase();

        return "CANCELED".equals(normalized)
                ? "CANCELLED"
                : normalized;
    }
}
