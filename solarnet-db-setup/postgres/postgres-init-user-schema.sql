CREATE SCHEMA IF NOT EXISTS solaruser;

ALTER DEFAULT PRIVILEGES IN SCHEMA solaruser REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solaruser REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solaruser REVOKE ALL ON FUNCTIONS FROM PUBLIC;