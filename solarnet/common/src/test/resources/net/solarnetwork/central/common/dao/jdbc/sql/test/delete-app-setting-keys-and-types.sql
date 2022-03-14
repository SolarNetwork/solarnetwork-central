DELETE FROM solarcommon.app_setting
WHERE skey = ANY(?)
	AND stype = ANY(?)