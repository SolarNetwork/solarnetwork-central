/* ============================================================================
 * psql script to execute all initialization SQL scripts, except for
 * populating any initial data (see postgres-init-data.sql for that).
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
