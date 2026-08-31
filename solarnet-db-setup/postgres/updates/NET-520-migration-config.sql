/**************************************************************************************************
 * FUNCTION solarcommon.db_migration_get_tag()
 *
 * Get the current DB migration tag.
 
 * @return the migration tag (if one is set)
 */
CREATE OR REPLACE FUNCTION solarcommon.db_migration_get_tag()
	RETURNS TEXT LANGUAGE SQL STABLE AS
$$
	SELECT svalue
	FROM solarcommon.app_setting
	WHERE skey = 'db'
	AND stype = 'migration'
	LIMIT 1
$$;

/**************************************************************************************************
 * FUNCTION solarcommon.db_migration_set_tag()
 *
 * Mark a DB migration with a specific tag.
 *
 * @param tag the tag to set
 * @return the updated app_setting row with the tag set
 */
CREATE OR REPLACE FUNCTION solarcommon.db_migration_set_tag(tag text)
	RETURNS SETOF solarcommon.app_setting LANGUAGE SQL VOLATILE ROWS 1 AS
$$
	INSERT INTO solarcommon.app_setting (skey, stype, svalue)
	VALUES ('db', 'migration', tag)
	ON CONFLICT (skey, stype) DO UPDATE
	SET modified = CURRENT_TIMESTAMP
		, svalue = EXCLUDED.svalue
	RETURNING app_setting.created
		, app_setting.modified
		, app_setting.skey
		, app_setting.stype
		, app_setting.svalue
$$;
