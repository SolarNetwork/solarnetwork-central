-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261102.sql

\i updates/NET-176-cross-account-source-aliases.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261102');
