-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261015.sql

\i updates/NET-531-to-basex.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261015');
