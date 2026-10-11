-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261011.sql

\i updates/NET-529-account-task-exec-at.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261011');
