-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261121.sql

\i updates/NET-545-store-loc-datum-array-fixes.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261121');
