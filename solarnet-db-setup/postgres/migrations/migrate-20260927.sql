-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260927.sql

\i updates/NET-527-audit-daily-rollup-tz.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260927');
