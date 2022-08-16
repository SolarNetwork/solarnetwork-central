UPDATE solaroscp.oscp_cp_conf SET
	  modified = ?
	, enabled = ?
	, reg_status = ?
	, cname = ?
	, url = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?