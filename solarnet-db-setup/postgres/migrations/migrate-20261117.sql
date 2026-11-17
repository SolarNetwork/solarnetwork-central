-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261117.sql

\i updates/NET-539-skip-unneeded-prior-datum-search.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261117');
