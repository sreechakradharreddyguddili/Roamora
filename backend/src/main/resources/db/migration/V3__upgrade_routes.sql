ALTER TABLE routes
ADD COLUMN user_id BIGINT;

ALTER TABLE routes
ADD COLUMN transport_mode VARCHAR(30) NOT NULL DEFAULT 'CAR';

ALTER TABLE routes
ADD COLUMN distance_km DOUBLE PRECISION;

ALTER TABLE routes
ADD COLUMN duration_minutes INTEGER;

ALTER TABLE routes
ADD COLUMN risk_score INTEGER;

ALTER TABLE routes
ADD COLUMN recommendation VARCHAR(1000);

ALTER TABLE routes
ADD COLUMN route_geometry TEXT;

ALTER TABLE routes
ADD CONSTRAINT fk_routes_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

CREATE INDEX idx_routes_user_id
ON routes(user_id);