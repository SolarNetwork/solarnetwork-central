-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260907.sql

\i updates/NET-509-html-stale-datum-alerts.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260907');
