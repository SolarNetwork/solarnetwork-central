SELECT s.created, s.modified, s.skey, s.stype, s.svalue
FROM solarcommon.app_setting s
WHERE s.skey = ?
	AND s.stype = ?