ALTER TABLE routes
ADD COLUMN saved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_routes_user_saved
ON routes(user_id, saved);