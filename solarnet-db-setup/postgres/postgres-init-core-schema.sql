CREATE SCHEMA IF NOT EXISTS solarnet;

ALTER DEFAULT PRIVILEGES IN SCHEMA solarnet REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solarnet REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solarnet REVOKE ALL ON FUNCTIONS FROM PUBLIC;