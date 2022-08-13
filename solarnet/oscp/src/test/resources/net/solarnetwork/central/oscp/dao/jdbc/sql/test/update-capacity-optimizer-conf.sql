UPDATE solaruser.user_oscp_co_conf SET
	  modified = ?
	, enabled = ?
	, reg_status = ?
	, cname = ?
	, url = ?
	, token = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?