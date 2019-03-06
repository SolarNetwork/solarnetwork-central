/* ============================================================================
 * psql script to execute all initialization SQL scripts, except for
 * populating any initial data (see postgres-init-data.sql for that).
 *
 * The postgres-init-plv8.sql script should be run PRIOR to running this.
 * ============================================================================
 */

\i postgres-init-common-schema.sql
\i postgres-init-common.sql
\i postgres-init-core-schema.sql
\i postgres-init-core.sql
\i postgres-init-instructor.sql
\i postgres-init-generic-datum-schema.sql
\i postgres-init-generic-datum.sql
\i postgres-init-generic-datum-agg-functions.sql
\i postgres-init-generic-datum-agg-triggers.sql
\i postgres-init-generic-loc-datum.sql
\i postgres-init-generic-loc-datum-agg-functions.sql
\i postgres-init-generic-loc-datum-agg-triggers.sql
\i postgres-init-generic-datum-x-functions.sql
\i postgres-init-generic-loc-datum-x-functions.sql
\i postgres-init-datum-export.sql
\i postgres-init-user-schema.sql
\i postgres-init-users.sql
\i postgres-init-user-alerts.sql
\i postgres-init-user-datum-export.sql
\i postgres-init-user-datum-expire.sql
\i postgres-init-user-datum-import.sql
\i postgres-init-controls.sql
\i postgres-init-quartz-schema.sql
\i postgres-init-quartz.sql
