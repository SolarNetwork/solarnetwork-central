/* ============================================================================
 * psql script to execute all initialization SQL scripts, except for
 * populating any initial data (see postgres-init-data.sql for that).
 * ============================================================================
 */

\i postgres-init-common-schema.sql
\i postgres-init-common.sql
\i postgres-init-core-schema.sql
\i postgres-init-core.sql
\i postgres-init-instructor.sql

\i postgres-init-datm-schema.sql
\i postgres-init-datm-core.sql
\i postgres-init-datm-util.sql
\i postgres-init-datm-query.sql
\i postgres-init-datm-agg-query.sql
\i postgres-init-datm-agg.sql
\i postgres-init-datm-agg-util.sql
\i postgres-init-datm-audit-query.sql
\i postgres-init-datm-audit.sql
\i postgres-init-datm-delete.sql
\i postgres-init-datm-in.sql
\i postgres-init-datm-in-loc.sql
\i postgres-init-datm-query-agg.sql
\i postgres-init-datm-query-diff.sql

\i postgres-init-datum-export.sql
\i postgres-init-user-schema.sql
\i postgres-init-users.sql
\i postgres-init-user-alerts.sql
\i postgres-init-user-datum-export.sql
\i postgres-init-user-datum-expire.sql
\i postgres-init-user-datum-flux.sql
\i postgres-init-user-datum-import.sql
\i postgres-init-user-event-hook.sql
\i postgres-init-user-events.sql
\i postgres-init-ocpp-schema.sql
\i postgres-init-ocpp.sql
\i postgres-init-oscp-schema.sql
\i postgres-init-oscp.sql
\i postgres-init-dnp3-schema.sql
\i postgres-init-dnp3.sql
\i postgres-init-din-schema.sql
\i postgres-init-din.sql

\i postgres-init-billing-schema.sql
\i postgres-init-billing.sql

