CREATE SCHEMA IF NOT EXISTS solardatum;

ALTER DEFAULT PRIVILEGES IN SCHEMA solardatum REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solardatum REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solardatum REVOKE ALL ON FUNCTIONS FROM PUBLIC;

CREATE SCHEMA IF NOT EXISTS solaragg;

ALTER DEFAULT PRIVILEGES IN SCHEMA solaragg REVOKE ALL ON TABLES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solaragg REVOKE ALL ON SEQUENCES FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA solaragg REVOKE ALL ON FUNCTIONS FROM PUBLIC;