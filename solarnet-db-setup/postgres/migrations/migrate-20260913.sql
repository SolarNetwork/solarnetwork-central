-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260913.sql

\i updates/NET-521-user-locale.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260913');
