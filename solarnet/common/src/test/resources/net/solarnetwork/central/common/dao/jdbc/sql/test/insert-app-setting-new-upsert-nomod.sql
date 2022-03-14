INSERT INTO solarcommon.app_setting (skey, stype, svalue)
VALUES (?,?,?)
ON CONFLICT (skey, stype) DO UPDATE
SET svalue = EXCLUDED.svalue