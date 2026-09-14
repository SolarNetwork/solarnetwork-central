/* ============================================================================
 * Fresh install migration version: this version should be maintained to match
 * whatever DDL migration changes have been merged into the setup DDL scripts.
 * ============================================================================
 */

SELECT svalue FROM solarcommon.db_migration_set_tag('20260913');
