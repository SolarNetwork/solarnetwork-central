-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261101.sql

\i updates/NET-176-alias-stream-access.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261101');
