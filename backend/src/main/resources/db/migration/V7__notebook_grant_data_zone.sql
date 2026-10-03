ALTER TABLE notebook_grants
  ADD COLUMN data_time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';
