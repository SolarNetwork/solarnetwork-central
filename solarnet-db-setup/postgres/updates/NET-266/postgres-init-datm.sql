--DROP SCHEMA IF EXISTS solardatm CASCADE;
CREATE SCHEMA IF NOT EXISTS solardatm;

\i postgres-init-datm-core.sql
\i postgres-init-datm-agg-query.sql
\i postgres-init-datm-agg.sql
\i postgres-init-datm-agg-util.sql
\i postgres-init-datm-audit-query.sql
\i postgres-init-datm-audit.sql
\i postgres-init-datm-delete.sql
\i postgres-init-datm-in.sql
\i postgres-init-datm-in-loc.sql
\i postgres-init-datm-migration.sql
\i postgres-init-datm-migration-loc.sql
\i postgres-init-datm-util.sql
\i postgres-init-datm-query.sql
\i postgres-init-datm-query-agg.sql
\i postgres-init-datm-query-diff.sql
