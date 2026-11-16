-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261116.sql

\i updates/NET-538-prior-datum-way-back-search.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261116');
