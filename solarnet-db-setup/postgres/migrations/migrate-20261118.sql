-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261118.sql

\i updates/NET-541-calc-stale-datm-scan-chunks-in-range.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261118');
