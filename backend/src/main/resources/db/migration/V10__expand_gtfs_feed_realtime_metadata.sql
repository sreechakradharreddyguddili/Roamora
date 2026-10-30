-- V10__expand_gtfs_feed_realtime_metadata.sql

ALTER TABLE gtfs_feed
    ADD COLUMN IF NOT EXISTS realtime_trip_updates_url TEXT;

ALTER TABLE gtfs_feed
    ADD COLUMN IF NOT EXISTS realtime_vehicle_positions_url TEXT;

ALTER TABLE gtfs_feed
    ADD COLUMN IF NOT EXISTS realtime_alerts_url TEXT;

ALTER TABLE gtfs_feed
    ADD COLUMN IF NOT EXISTS license_url TEXT;

ALTER TABLE gtfs_feed
    ADD COLUMN IF NOT EXISTS realtime_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_gtfs_feed_realtime_enabled
    ON gtfs_feed(active, realtime_enabled);
