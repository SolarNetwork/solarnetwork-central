-- Run this script from the parent directory, e.g. psql -f migrations/migrate-20261126.sql

\i updates/NET-540-rollup-datm-slots-hour-rule.sql

SELECT svalue FROM solarcommon.db_migration_set_tag('20261126');
