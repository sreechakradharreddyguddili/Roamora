ALTER TABLE gtfs_route
    ADD COLUMN IF NOT EXISTS route_color VARCHAR(20);

ALTER TABLE gtfs_route
    ADD COLUMN IF NOT EXISTS route_text_color VARCHAR(20);

ALTER TABLE gtfs_route
    ADD COLUMN IF NOT EXISTS route_url TEXT;

ALTER TABLE gtfs_stop
    ADD COLUMN IF NOT EXISTS platform_code VARCHAR(100);

ALTER TABLE gtfs_stop
    ADD COLUMN IF NOT EXISTS level_id VARCHAR(100);

ALTER TABLE gtfs_stop
    ADD COLUMN IF NOT EXISTS wheelchair_boarding VARCHAR(20);

ALTER TABLE gtfs_stop
    ADD COLUMN IF NOT EXISTS zone_id VARCHAR(255);

ALTER TABLE gtfs_stop_time
    ADD COLUMN IF NOT EXISTS pickup_type VARCHAR(20);

ALTER TABLE gtfs_stop_time
    ADD COLUMN IF NOT EXISTS drop_off_type VARCHAR(20);

ALTER TABLE gtfs_stop_time
    ADD COLUMN IF NOT EXISTS shape_dist_traveled VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_parent_station
    ON gtfs_stop(parent_station);

CREATE INDEX IF NOT EXISTS idx_gtfs_stop_code
    ON gtfs_stop(stop_code);

CREATE INDEX IF NOT EXISTS idx_gtfs_route_type
    ON gtfs_route(route_type);