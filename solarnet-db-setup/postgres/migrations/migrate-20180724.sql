-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20180724.sql
--
-- NOTE: the plv8 modules must be re-loaded first; e.g.
--
-- 		cd plv8
-- 		psql -U postgres -d solarnetwork -f postgres-init-plv8-modules.sql

\i updates/NET-153-refactor-agg-processing.sql
