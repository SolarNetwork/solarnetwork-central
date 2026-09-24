-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260924.sql

\i updates/NET-451-datum-export-task-user.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260924');
