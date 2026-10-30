CREATE TABLE IF NOT EXISTS gtfs_calendar (
    feed_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,

    monday BOOLEAN NOT NULL DEFAULT FALSE,
    tuesday BOOLEAN NOT NULL DEFAULT FALSE,
    wednesday BOOLEAN NOT NULL DEFAULT FALSE,
    thursday BOOLEAN NOT NULL DEFAULT FALSE,
    friday BOOLEAN NOT NULL DEFAULT FALSE,
    saturday BOOLEAN NOT NULL DEFAULT FALSE,
    sunday BOOLEAN NOT NULL DEFAULT FALSE,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    PRIMARY KEY (
        feed_id,
        service_id
    ),

    CONSTRAINT fk_gtfs_calendar_feed
        FOREIGN KEY (feed_id)
        REFERENCES gtfs_feed(feed_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gtfs_calendar_dates
    ON gtfs_calendar (
        feed_id,
        start_date,
        end_date
    );


CREATE TABLE IF NOT EXISTS gtfs_calendar_date (
    feed_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    exception_date DATE NOT NULL,

    exception_type INTEGER NOT NULL,

    PRIMARY KEY (
        feed_id,
        service_id,
        exception_date
    ),

    CONSTRAINT fk_gtfs_calendar_date_feed
        FOREIGN KEY (feed_id)
        REFERENCES gtfs_feed(feed_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gtfs_calendar_date_lookup
    ON gtfs_calendar_date (
        feed_id,
        exception_date,
        service_id
    );