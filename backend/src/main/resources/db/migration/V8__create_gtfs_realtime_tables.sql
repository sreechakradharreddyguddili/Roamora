CREATE TABLE IF NOT EXISTS gtfs_realtime_trip_update (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    trip_id VARCHAR(255) NOT NULL,

    route_id VARCHAR(255),

    stop_id VARCHAR(255),

    stop_sequence INTEGER,

    scheduled_time TIMESTAMP WITH TIME ZONE,

    estimated_time TIMESTAMP WITH TIME ZONE,

    delay_seconds INTEGER,

    status VARCHAR(50),

    fetched_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gtfs_rt_trip
    ON gtfs_realtime_trip_update(feed_id, trip_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_rt_stop
    ON gtfs_realtime_trip_update(feed_id, stop_id);

CREATE TABLE IF NOT EXISTS gtfs_realtime_vehicle (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    vehicle_id VARCHAR(255),

    trip_id VARCHAR(255),

    route_id VARCHAR(255),

    latitude DOUBLE PRECISION,

    longitude DOUBLE PRECISION,

    bearing DOUBLE PRECISION,

    speed DOUBLE PRECISION,

    current_stop_sequence INTEGER,

    current_status VARCHAR(50),

    fetched_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gtfs_rt_vehicle_feed
    ON gtfs_realtime_vehicle(feed_id);

CREATE INDEX IF NOT EXISTS idx_gtfs_rt_vehicle_trip
    ON gtfs_realtime_vehicle(feed_id, trip_id);

CREATE TABLE IF NOT EXISTS gtfs_realtime_alert (
    id BIGSERIAL PRIMARY KEY,

    feed_id VARCHAR(255) NOT NULL,

    alert_id VARCHAR(255),

    cause VARCHAR(100),

    effect VARCHAR(100),

    header_text TEXT,

    description_text TEXT,

    url TEXT,

    active_from TIMESTAMP WITH TIME ZONE,

    active_until TIMESTAMP WITH TIME ZONE,

    fetched_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gtfs_rt_alert_feed
    ON gtfs_realtime_alert(feed_id);