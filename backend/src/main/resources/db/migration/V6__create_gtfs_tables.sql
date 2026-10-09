CREATE TABLE IF NOT EXISTS gtfs_feed (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL UNIQUE,

    provider_name VARCHAR(255),

    country VARCHAR(255),

    country_code VARCHAR(10),

    region VARCHAR(255),

    city VARCHAR(255),

    feed_url TEXT NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS gtfs_stop (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    stop_id VARCHAR(255) NOT NULL,

    stop_code VARCHAR(255),

    stop_name VARCHAR(500) NOT NULL,

    latitude DOUBLE PRECISION,

    longitude DOUBLE PRECISION,

    location_type VARCHAR(50),

    parent_station VARCHAR(255),

    timezone VARCHAR(100),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_gtfs_stop
        UNIQUE (feed_id, stop_id)
);


CREATE TABLE IF NOT EXISTS gtfs_route (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    route_id VARCHAR(255) NOT NULL,

    route_short_name VARCHAR(255),

    route_long_name VARCHAR(500),

    route_type VARCHAR(50),

    agency_id VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_gtfs_route
        UNIQUE (feed_id, route_id)
);


CREATE TABLE IF NOT EXISTS gtfs_trip (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    trip_id VARCHAR(255) NOT NULL,

    route_id VARCHAR(255),

    service_id VARCHAR(255),

    trip_headsign VARCHAR(500),

    direction_id VARCHAR(50),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_gtfs_trip
        UNIQUE (feed_id, trip_id)
);


CREATE TABLE IF NOT EXISTS gtfs_stop_time (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    trip_id VARCHAR(255) NOT NULL,

    stop_id VARCHAR(255) NOT NULL,

    arrival_time VARCHAR(20),

    departure_time VARCHAR(20),

    stop_sequence INTEGER,

    stop_headsign VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_name
    ON gtfs_stop (stop_name);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_location
    ON gtfs_stop (latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_feed
    ON gtfs_stop (feed_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_route_feed
    ON gtfs_route (feed_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_trip_route
    ON gtfs_trip (feed_id, route_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_time_trip
    ON gtfs_stop_time (feed_id, trip_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_time_stop
    ON gtfs_stop_time (feed_id, stop_id);