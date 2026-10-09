package com.weatherwise.transport.global;

import com.weatherwise.enums.TransportMode;
import com.weatherwise.enums.TransportPlaceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class GlobalTransportPlaceRepository {

    private final JdbcTemplate jdbcTemplate;

    public GlobalTransportPlaceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(GlobalTransportPlace place) {
        jdbcTemplate.update("""
            INSERT INTO global_transport_place (
                source, source_id, name, place_type,
                city, country, country_code, region,
                latitude, longitude, timezone, code,
                operator_name, parent_source_id,
                airport_type, icao_code, iata_code,
                website, active, metadata, updated_at
            )
            VALUES (
                ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?,
                ?, ?, ?,
                ?, TRUE, '{}'::jsonb, CURRENT_TIMESTAMP
            )
            ON CONFLICT (source, source_id)
            DO UPDATE SET
                name = EXCLUDED.name,
                place_type = EXCLUDED.place_type,
                city = EXCLUDED.city,
                country = EXCLUDED.country,
                country_code = EXCLUDED.country_code,
                region = EXCLUDED.region,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                timezone = EXCLUDED.timezone,
                code = EXCLUDED.code,
                operator_name = EXCLUDED.operator_name,
                parent_source_id = EXCLUDED.parent_source_id,
                airport_type = EXCLUDED.airport_type,
                icao_code = EXCLUDED.icao_code,
                iata_code = EXCLUDED.iata_code,
                website = EXCLUDED.website,
                active = TRUE,
                updated_at = CURRENT_TIMESTAMP
            """,
            place.source(), place.sourceId(), place.name(), place.type().name(),
            place.city(), place.country(), place.countryCode(), place.region(),
            place.latitude(), place.longitude(), place.timezone(), place.code(),
            place.operatorName(), place.parentSourceId(), place.airportType(),
            place.icaoCode(), place.iataCode(), place.website()
        );
    }

    public List<GlobalTransportPlace> search(
            String query, String city, String country,
            TransportPlaceType type, int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));

        StringBuilder sql = new StringBuilder("""
            SELECT source, source_id, name, place_type,
                   city, country, country_code, region,
                   latitude, longitude, timezone, code,
                   operator_name, parent_source_id,
                   airport_type, icao_code, iata_code, website
            FROM global_transport_place
            WHERE active = TRUE
            """);

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append("""
                AND (
                    LOWER(name) LIKE LOWER(?)
                    OR LOWER(COALESCE(code, '')) LIKE LOWER(?)
                    OR LOWER(COALESCE(iata_code, '')) LIKE LOWER(?)
                    OR LOWER(COALESCE(icao_code, '')) LIKE LOWER(?)
                )
                """);
            String value = "%" + query.trim() + "%";
            params.add(value);
            params.add(value);
            params.add(value);
            params.add(value);
        }

        if (city != null && !city.isBlank()) {
            sql.append(" AND LOWER(COALESCE(city, '')) = LOWER(?) ");
            params.add(city.trim());
        }

        if (country != null && !country.isBlank()) {
            sql.append("""
                AND (
                    LOWER(COALESCE(country, '')) = LOWER(?)
                    OR LOWER(COALESCE(country_code, '')) = LOWER(?)
                )
                """);
            params.add(country.trim());
            params.add(country.trim());
        }

        if (type != null) {
            sql.append(" AND place_type = ? ");
            params.add(type.name());
        }

        sql.append("""
            ORDER BY
                CASE
                    WHEN LOWER(name) = LOWER(?) THEN 0
                    WHEN LOWER(name) LIKE LOWER(?) THEN 1
                    WHEN LOWER(COALESCE(iata_code, '')) = LOWER(?) THEN 0
                    WHEN LOWER(COALESCE(icao_code, '')) = LOWER(?) THEN 0
                    ELSE 2
                END,
                name
            LIMIT ?
            """);

        String exact = query == null ? "" : query.trim();
        params.add(exact);
        params.add(exact + "%");
        params.add(exact);
        params.add(exact);
        params.add(safeLimit);

        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (rs, rowNum) -> map(rs));
    }

    public List<GlobalTransportPlace> nearby(
            double latitude, double longitude,
            TransportPlaceType type, double radiusKm, int limit
    ) {
        double safeRadius = Math.max(0.1, Math.min(radiusKm, 200.0));
        int safeLimit = Math.max(1, Math.min(limit, 100));

        double latDelta = safeRadius / 111.0;
        double lonDelta = safeRadius /
                (111.0 * Math.max(Math.cos(Math.toRadians(latitude)), 0.01));

        StringBuilder sql = new StringBuilder("""
            SELECT source, source_id, name, place_type,
                   city, country, country_code, region,
                   latitude, longitude, timezone, code,
                   operator_name, parent_source_id,
                   airport_type, icao_code, iata_code, website
            FROM global_transport_place
            WHERE active = TRUE
              AND latitude IS NOT NULL
              AND longitude IS NOT NULL
              AND latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(latitude - latDelta);
        params.add(latitude + latDelta);
        params.add(longitude - lonDelta);
        params.add(longitude + lonDelta);

        if (type != null) {
            sql.append(" AND place_type = ? ");
            params.add(type.name());
        }

        sql.append("""
            AND (
                6371.0 * 2.0 * ASIN(
                    SQRT(
                        POWER(SIN(RADIANS(latitude - ?) / 2), 2)
                        +
                        COS(RADIANS(?))
                        * COS(RADIANS(latitude))
                        * POWER(SIN(RADIANS(longitude - ?) / 2), 2)
                    )
                )
            ) <= ?
            ORDER BY (
                6371.0 * 2.0 * ASIN(
                    SQRT(
                        POWER(SIN(RADIANS(latitude - ?) / 2), 2)
                        +
                        COS(RADIANS(?))
                        * COS(RADIANS(latitude))
                        * POWER(SIN(RADIANS(longitude - ?) / 2), 2)
                    )
                )
            )
            LIMIT ?
            """);

        params.add(latitude);
        params.add(latitude);
        params.add(longitude);
        params.add(safeRadius);
        params.add(latitude);
        params.add(latitude);
        params.add(longitude);
        params.add(safeLimit);

        return jdbcTemplate.query(sql.toString(), params.toArray(),
                (rs, rowNum) -> map(rs));
    }

    private GlobalTransportPlace map(java.sql.ResultSet rs)
            throws java.sql.SQLException {

        TransportPlaceType type =
                TransportPlaceType.valueOf(rs.getString("place_type"));

        List<TransportMode> modes = switch (type) {
            case AIRPORT -> List.of(TransportMode.FLIGHT);
            case RAILWAY_STATION -> List.of(
                    TransportMode.TRAIN,
                    TransportMode.COMMUTER_RAIL,
                    TransportMode.HIGH_SPEED_TRAIN
            );
            case METRO_STATION, SUBWAY_STATION ->
                    List.of(TransportMode.METRO, TransportMode.SUBWAY);
            case TRAM_STOP, LIGHT_RAIL_STATION ->
                    List.of(TransportMode.TRAM, TransportMode.LIGHT_RAIL);
            case MONORAIL_STATION -> List.of(TransportMode.MONORAIL);
            case BUS_STOP, BUS_TERMINAL, COACH_STOP, COACH_TERMINAL ->
                    List.of(TransportMode.BUS, TransportMode.COACH);
            case FERRY_TERMINAL, PORT ->
                    List.of(TransportMode.FERRY, TransportMode.SHIP);
            case TAXI_STAND -> List.of(TransportMode.TAXI);
            case CAR_RENTAL -> List.of(TransportMode.CAR);
            case BICYCLE_STATION -> List.of(TransportMode.BICYCLE);
            case SCOOTER_STATION -> List.of(TransportMode.SCOOTER);
        };

        return new GlobalTransportPlace(
                rs.getString("source"),
                rs.getString("source_id"),
                rs.getString("name"),
                type,
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("country_code"),
                rs.getString("region"),
                getDouble(rs, "latitude"),
                getDouble(rs, "longitude"),
                rs.getString("timezone"),
                rs.getString("code"),
                rs.getString("operator_name"),
                rs.getString("parent_source_id"),
                rs.getString("airport_type"),
                rs.getString("icao_code"),
                rs.getString("iata_code"),
                rs.getString("website"),
                modes
        );
    }

    private Double getDouble(java.sql.ResultSet rs, String column)
            throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
