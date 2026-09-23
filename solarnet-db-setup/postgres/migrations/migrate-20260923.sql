-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260923.sql

\i updates/NET-526-ant-pattern-segment-match.sql
\i updates/NET-525-jsonb-prune-paths.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260923');
