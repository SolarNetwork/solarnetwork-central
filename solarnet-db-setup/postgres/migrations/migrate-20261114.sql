-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261114.sql

\i updates/NET-537-hour-agg-next-datum-require-accumulating.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261114');
