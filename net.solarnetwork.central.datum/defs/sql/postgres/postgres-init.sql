/* ============================================================================
 * psql script to execute all initialization SQL scripts, except for
 * populating any initial data (see postgres-init-data.sql for that).
 * 
 * The postgres-init-plv8.sql script should be run PRIOR to running this.
 * ============================================================================
 */

\i postgres-init-core.sql
\i postgres-init-instructor.sql
\i postgres-init-users.sql
\i postgres-init-controls.sql
\i postgres-init-most-recent.sql
\i postgres-init-reporting-tables.sql
\i postgres-init-reporting-triggers.sql
\i postgres-init-quartz.sql
\i postgres-init-common.sql
\i postgres-init-generic-datum.sql
\i postgres-init-generic-datum-agg-functions.sql
\i postgres-init-generic-datum-agg-triggers.sql
\i postgres-init-generic-datum-migration.sql
\i postgres-init-generic-datum-migration-triggers.sql
