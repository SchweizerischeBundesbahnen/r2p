-- Add a monotonically increasing sequence column to changelog to guarantee deterministic ordering
-- when multiple entries share the same timestamp (e.g. within a single transaction).
ALTER TABLE changelog ADD COLUMN seq BIGSERIAL;

