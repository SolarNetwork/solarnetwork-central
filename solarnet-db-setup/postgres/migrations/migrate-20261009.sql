-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261009.sql

\i updates/NET-528-node-public-ssh-key.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261009');
