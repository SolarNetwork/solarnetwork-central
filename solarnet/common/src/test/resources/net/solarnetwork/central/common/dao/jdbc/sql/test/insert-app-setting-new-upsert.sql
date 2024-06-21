INSERT INTO solarcommon.app_setting (created, modified, skey, stype, svalue)
VALUES (?,?,?,?,?)
ON CONFLICT (skey, stype) DO UPDATE
SET modified = EXCLUDED.modified
	, svalue = EXCLUDED.svalue