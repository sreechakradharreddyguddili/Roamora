package com.weatherwise.transport.gtfs;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import com.weatherwise.location.TransportPlace;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GtfsSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public GtfsSearchRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                jdbcTemplate;
    }

    public List<TransportPlace> searchPlaces(
            String city,
            String country,
            TransportPlaceType type,
            String query
    ) {

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT DISTINCT
                            s.feed_id,
                            s.stop_id,
                            s.stop_code,
                            s.stop_name,
                            s.latitude,
                            s.longitude,
                            s.location_type,
                            s.parent_station,
                            s.timezone,
                            s.platform_code,
                            s.level_id,
                            s.wheelchair_boarding,
                            s.zone_id,

                            f.country,
                            f.country_code,
                            f.city,
                            f.region,
                            f.provider_name,

                            (
                                SELECT ARRAY_AGG(
                                    DISTINCT r2.route_type
                                )
                                FROM gtfs_stop_time st2
                                JOIN gtfs_trip t2
                                    ON t2.feed_id = st2.feed_id
                                    AND t2.trip_id = st2.trip_id
                                JOIN gtfs_route r2
                                    ON r2.feed_id = t2.feed_id
                                    AND r2.route_id = t2.route_id
                                WHERE st2.feed_id = s.feed_id
                                  AND st2.stop_id = s.stop_id
                                  AND r2.route_type IS NOT NULL
                            ) AS route_types

                        FROM gtfs_stop s

                        JOIN gtfs_feed f
                            ON f.feed_id = s.feed_id

                        LEFT JOIN gtfs_stop_time st
                            ON st.feed_id = s.feed_id
                            AND st.stop_id = s.stop_id

                        LEFT JOIN gtfs_trip t
                            ON t.feed_id = st.feed_id
                            AND t.trip_id = st.trip_id

                        LEFT JOIN gtfs_route r
                            ON r.feed_id = t.feed_id
                            AND r.route_id = t.route_id

                        WHERE f.active = TRUE
                        """
                );

        List<Object> parameters =
                new ArrayList<>();

        /*
         * Country filter.
         */
        if (country != null
                && !country.isBlank()) {

            sql.append(
                    """
                    AND (
                        LOWER(f.country) = LOWER(?)
                        OR LOWER(f.country_code) = LOWER(?)
                    )
                    """
            );

            parameters.add(
                    country.trim()
            );

            parameters.add(
                    country.trim()
            );
        }

        /*
         * City filter.
         */
        if (city != null
                && !city.isBlank()) {

            sql.append(
                    """
                    AND (
                        LOWER(f.city) = LOWER(?)
                        OR LOWER(s.stop_name)
                            LIKE LOWER(?)
                    )
                    """
            );

            parameters.add(
                    city.trim()
            );

            parameters.add(
                    "%" + city.trim() + "%"
            );
        }

        /*
         * Specific stop/station search.
         */
        if (query != null
                && !query.isBlank()) {

            sql.append(
                    """
                    AND (
                        LOWER(s.stop_name)
                            LIKE LOWER(?)

                        OR LOWER(
                            COALESCE(
                                s.stop_code,
                                ''
                            )
                        ) LIKE LOWER(?)
                    )
                    """
            );

            String search =
                    "%" + query.trim() + "%";

            parameters.add(search);
            parameters.add(search);
        }

        /*
         * Transport-place type filter.
         */
        appendTransportTypeFilter(
                sql,
                parameters,
                type
        );

        /*
         * Search ordering.
         */
        sql.append(
                """
                ORDER BY
                    CASE
                        WHEN LOWER(s.stop_name)
                             = LOWER(?) THEN 0

                        WHEN LOWER(s.stop_name)
                             LIKE LOWER(?) THEN 1

                        ELSE 2
                    END,

                    s.stop_name

                LIMIT 100
                """
        );

        String exactQuery =
                query == null
                        ? ""
                        : query.trim();

        parameters.add(
                exactQuery
        );

        parameters.add(
                exactQuery + "%"
        );

        return jdbcTemplate.query(
                sql.toString(),
                parameters.toArray(),
                (rs, rowNum) ->
                        mapTransportPlace(
                                rs,
                                type
                        )
        );
    }

    /*
     * Find transport places near coordinates.
     */
    public List<TransportPlace> searchNearby(
            double latitude,
            double longitude,
            TransportPlaceType type,
            double radiusKm,
            int limit
    ) {

        double safeRadius =
                Math.max(
                        0.1,
                        Math.min(
                                radiusKm,
                                100.0
                        )
                );

        int safeLimit =
                Math.max(
                        1,
                        Math.min(
                                limit,
                                100
                        )
                );

        /*
         * Bounding box optimisation.
         */
        double latitudeDelta =
                safeRadius / 111.0;

        double longitudeDelta =
                safeRadius /
                        (
                                111.0 *
                                Math.max(
                                        Math.cos(
                                                Math.toRadians(
                                                        latitude
                                                )
                                        ),
                                        0.01
                                )
                        );

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT DISTINCT
                            s.feed_id,
                            s.stop_id,
                            s.stop_code,
                            s.stop_name,
                            s.latitude,
                            s.longitude,
                            s.location_type,
                            s.parent_station,
                            s.timezone,
                            s.platform_code,
                            s.level_id,
                            s.wheelchair_boarding,
                            s.zone_id,

                            f.country,
                            f.country_code,
                            f.city,
                            f.region,
                            f.provider_name,

                            (
                                SELECT ARRAY_AGG(
                                    DISTINCT r2.route_type
                                )
                                FROM gtfs_stop_time st2
                                JOIN gtfs_trip t2
                                    ON t2.feed_id = st2.feed_id
                                    AND t2.trip_id = st2.trip_id
                                JOIN gtfs_route r2
                                    ON r2.feed_id = t2.feed_id
                                    AND r2.route_id = t2.route_id
                                WHERE st2.feed_id = s.feed_id
                                  AND st2.stop_id = s.stop_id
                                  AND r2.route_type IS NOT NULL
                            ) AS route_types,

                            (
                                6371.0 * 2.0 * ASIN(
                                    SQRT(
                                        POWER(
                                            SIN(
                                                RADIANS(
                                                    s.latitude - ?
                                                ) / 2
                                            ),
                                            2
                                        )
                                        +
                                        COS(
                                            RADIANS(?)
                                        )
                                        *
                                        COS(
                                            RADIANS(
                                                s.latitude
                                            )
                                        )
                                        *
                                        POWER(
                                            SIN(
                                                RADIANS(
                                                    s.longitude - ?
                                                ) / 2
                                            ),
                                            2
                                        )
                                    )
                                )
                            ) AS distance_km

                        FROM gtfs_stop s

                        JOIN gtfs_feed f
                            ON f.feed_id = s.feed_id

                        LEFT JOIN gtfs_stop_time st
                            ON st.feed_id = s.feed_id
                            AND st.stop_id = s.stop_id

                        LEFT JOIN gtfs_trip t
                            ON t.feed_id = st.feed_id
                            AND t.trip_id = st.trip_id

                        LEFT JOIN gtfs_route r
                            ON r.feed_id = t.feed_id
                            AND r.route_id = t.route_id

                        WHERE f.active = TRUE

                          AND s.latitude IS NOT NULL
                          AND s.longitude IS NOT NULL

                          AND s.latitude BETWEEN ?
                              AND ?

                          AND s.longitude BETWEEN ?
                              AND ?
                        """
                );

        List<Object> parameters =
                new ArrayList<>();

        /*
         * Distance calculation.
         */
        parameters.add(latitude);
        parameters.add(latitude);
        parameters.add(longitude);

        /*
         * Bounding box.
         */
        parameters.add(
                latitude - latitudeDelta
        );

        parameters.add(
                latitude + latitudeDelta
        );

        parameters.add(
                longitude - longitudeDelta
        );

        parameters.add(
                longitude + longitudeDelta
        );

        /*
         * Transport type filtering.
         */
        appendTransportTypeFilter(
                sql,
                parameters,
                type
        );

        /*
         * Exact radius filter.
         */
        sql.append(
                """
                AND (
                    6371.0 * 2.0 * ASIN(
                        SQRT(
                            POWER(
                                SIN(
                                    RADIANS(
                                        s.latitude - ?
                                    ) / 2
                                ),
                                2
                            )
                            +
                            COS(
                                RADIANS(?)
                            )
                            *
                            COS(
                                RADIANS(
                                    s.latitude
                                )
                            )
                            *
                            POWER(
                                SIN(
                                    RADIANS(
                                        s.longitude - ?
                                    ) / 2
                                ),
                                2
                            )
                        )
                    )
                ) <= ?

                ORDER BY distance_km

                LIMIT ?
                """
        );

        parameters.add(latitude);
        parameters.add(latitude);
        parameters.add(longitude);
        parameters.add(safeRadius);
        parameters.add(safeLimit);

        return jdbcTemplate.query(
                sql.toString(),
                parameters.toArray(),
                (rs, rowNum) ->
                        mapTransportPlace(
                                rs,
                                type
                        )
        );
    }

    /*
     * Transport mode filtering.
     */
    private void appendTransportTypeFilter(
            StringBuilder sql,
            List<Object> parameters,
            TransportPlaceType type
    ) {

        if (type == null) {
            return;
        }

        String routeTypes =
                routeTypesFor(type);

        if (!routeTypes.isBlank()) {

            sql.append(
                    " AND r.route_type IN (" +
                            routeTypes +
                            ") "
            );
            return;
        }

        /*
         * AIRPORT, TAXI_STAND, CAR_RENTAL, BICYCLE_STATION
         * and SCOOTER_STATION are not reliably represented
         * by standard GTFS route_type values.
         *
         * Do not return unrelated bus/train records for these
         * requested types. Dedicated provider data will be
         * connected later.
         */
        if (type == TransportPlaceType.AIRPORT
                || type == TransportPlaceType.TAXI_STAND
                || type == TransportPlaceType.CAR_RENTAL
                || type == TransportPlaceType.BICYCLE_STATION
                || type == TransportPlaceType.SCOOTER_STATION) {

            sql.append(" AND 1 = 0 ");
        }
    }

    /*
     * Convert database result into TransportPlace.
     */
    private TransportPlace mapTransportPlace(
            ResultSet rs,
            TransportPlaceType requestedType
    ) throws SQLException {

        List<TransportMode> supportedModes =
                resolveTransportModes(
                        rs.getArray(
                                "route_types"
                        )
                );

        TransportPlaceType resolvedType =
                resolvePlaceType(
                        rs.getString(
                                "location_type"
                        ),
                        supportedModes,
                        requestedType
                );

        return new TransportPlace(
                rs.getString("feed_id"),
                rs.getString("stop_id"),
                rs.getString("stop_name"),
                resolvedType,

                rs.getString("city"),
                rs.getString("country"),
                rs.getString("country_code"),
                rs.getString("region"),

                getDouble(
                        rs,
                        "latitude"
                ),

                getDouble(
                        rs,
                        "longitude"
                ),

                rs.getString("timezone"),
                rs.getString("stop_code"),

                rs.getString("provider_name"),
                rs.getString("parent_station"),

                supportedModes
        );
    }

    /*
     * Convert GTFS route_type values into
     * our application's TransportMode values.
     */
    private List<TransportMode> resolveTransportModes(
            Array sqlArray
    ) throws SQLException {

        if (sqlArray == null) {
            return List.of();
        }

        Object value =
                sqlArray.getArray();

        if (!(value instanceof Object[] values)) {
            return List.of();
        }

        List<TransportMode> modes =
                new ArrayList<>();

        for (Object item : values) {

            if (item == null) {
                continue;
            }

            String routeType =
                    item.toString();

            TransportMode mode =
                    switch (routeType) {

                        case "0" ->
                                TransportMode.TRAM;

                        case "1" ->
                                TransportMode.SUBWAY;

                        case "2" ->
                                TransportMode.TRAIN;

                        case "3" ->
                                TransportMode.BUS;

                        case "4" ->
                                TransportMode.FERRY;

                        case "5" ->
                                TransportMode.TRAM;

                        case "6" ->
                                TransportMode.MONORAIL;

                        case "7" ->
                                TransportMode.TRAIN;

                        case "11" ->
                                TransportMode.BUS;

                        case "12" ->
                                TransportMode.MONORAIL;

                        default ->
                                null;
                    };

            if (mode != null
                    && !modes.contains(mode)) {

                modes.add(mode);
            }
        }

        return List.copyOf(modes);
    }

    /*
     * Resolve the primary place type.
     */
    private TransportPlaceType resolvePlaceType(
            String locationType,
            List<TransportMode> modes,
            TransportPlaceType requestedType
    ) {

        /*
         * Explicit user request always wins.
         */
        if (requestedType != null) {
            return requestedType;
        }

        if (modes.contains(
                TransportMode.SUBWAY
        )) {
            return TransportPlaceType.SUBWAY_STATION;
        }

        if (modes.contains(
                TransportMode.TRAM
        )) {
            return TransportPlaceType.TRAM_STOP;
        }

        if (modes.contains(
                TransportMode.FERRY
        )) {
            return TransportPlaceType.FERRY_TERMINAL;
        }

        if (modes.contains(
                TransportMode.TRAIN
        )) {
            return TransportPlaceType.RAILWAY_STATION;
        }

        if (modes.contains(
                TransportMode.BUS
        )) {
            return TransportPlaceType.BUS_STOP;
        }

        /*
         * GTFS station with no route information.
         */
        if ("1".equals(locationType)) {
            return TransportPlaceType.RAILWAY_STATION;
        }

        return TransportPlaceType.BUS_STOP;
    }

    /*
     * Map application transport-place types
     * to GTFS route_type values.
     */
    private String routeTypesFor(
            TransportPlaceType type
    ) {

        return switch (type) {

            case BUS_STOP,
                 BUS_TERMINAL ->
                    "'3'";

            /*
             * GTFS route_type does not have a universal
             * separate COACH value. Coach services are
             * therefore represented by the bus family when
             * a provider publishes them through GTFS.
             */
            case COACH_STOP,
                 COACH_TERMINAL ->
                    "'3'";

            case METRO_STATION,
                 SUBWAY_STATION ->
                    "'1'";

            case TRAM_STOP,
                 LIGHT_RAIL_STATION ->
                    "'0'";

            case RAILWAY_STATION ->
                    "'2','7'";

            case MONORAIL_STATION ->
                    "'6','12'";

            case FERRY_TERMINAL,
                 PORT ->
                    "'4'";

            /*
             * These are not reliably identifiable from
             * standard GTFS route_type values alone.
             * They require a dedicated place/airport/
             * micromobility provider.
             */
            case AIRPORT,
                 TAXI_STAND,
                 CAR_RENTAL,
                 BICYCLE_STATION,
                 SCOOTER_STATION ->
                    "";
        };
    }

    private Double getDouble(
            ResultSet rs,
            String column
    ) throws SQLException {

        double value =
                rs.getDouble(column);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }
}