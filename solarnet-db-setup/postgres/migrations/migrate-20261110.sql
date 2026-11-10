-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261110.sql

\i updates/NET-536-status-datum-prop-bug.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261110');
