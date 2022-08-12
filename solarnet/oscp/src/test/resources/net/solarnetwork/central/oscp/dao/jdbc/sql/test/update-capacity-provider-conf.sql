UPDATE solaruser.user_oscp_cp_conf SET
	  modified = ?
	, enabled = ?
	, reg_status = ?
	, cname = ?
	, url = ?
	, token = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?