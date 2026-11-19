-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261120.sql

\i updates/NET-540-agg-datm-15m.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261120');
