-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261119.sql

\i updates/NET-543-hour-agg-reset-pair-after-end.sql
\i updates/NET-544-day-month-agg-reading-start-end.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261119');
