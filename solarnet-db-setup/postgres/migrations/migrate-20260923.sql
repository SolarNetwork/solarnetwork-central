-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260923.sql

\i updates/NET-526-ant-pattern-segment-match.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260923');
