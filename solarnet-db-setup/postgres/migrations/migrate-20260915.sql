-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20260915.sql

\i updates/NET-523-token-auth-user-enabled.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20260915');
