CREATE TABLE global_transport_place (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(40) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    name VARCHAR(500) NOT NULL,
    place_type VARCHAR(50) NOT NULL,
    city VARCHAR(255),
    country VARCHAR(255),
    country_code VARCHAR(16),
    region VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    timezone VARCHAR(100),
    code VARCHAR(100),
    operator_name VARCHAR(500),
    parent_source_id VARCHAR(255),
    airport_type VARCHAR(80),
    icao_code VARCHAR(20),
    iata_code VARCHAR(20),
    website VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_global_transport_place_source UNIQUE (source, source_id)
);

CREATE INDEX idx_global_transport_place_name
    ON global_transport_place (LOWER(name));
CREATE INDEX idx_global_transport_place_city_country
    ON global_transport_place (LOWER(city), LOWER(country));
CREATE INDEX idx_global_transport_place_type
    ON global_transport_place (place_type);
CREATE INDEX idx_global_transport_place_country
    ON global_transport_place (LOWER(country), LOWER(country_code));
CREATE INDEX idx_global_transport_place_coordinates
    ON global_transport_place (latitude, longitude);
CREATE INDEX idx_global_transport_place_codes
    ON global_transport_place (iata_code, icao_code, code);
CREATE INDEX idx_global_transport_place_active
    ON global_transport_place (active);
