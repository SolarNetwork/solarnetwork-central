-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261031.sql

\i updates/NET-532-alias-query-accounting.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261031');
