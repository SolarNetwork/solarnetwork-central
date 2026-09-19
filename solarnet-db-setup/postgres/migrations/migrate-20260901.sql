-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260901.sql

\i updates/NET-520-migration-config.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260901');
