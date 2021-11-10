INSERT INTO solarcommon.app_setting (modified, skey, stype, svalue)
VALUES (?,?,?,?)
ON CONFLICT (skey, stype) DO UPDATE
SET modified = EXCLUDED.modified
	, svalue = EXCLUDED.svalue