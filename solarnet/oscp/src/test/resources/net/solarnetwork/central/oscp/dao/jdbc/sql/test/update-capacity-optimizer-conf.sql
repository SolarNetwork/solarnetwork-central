UPDATE solaroscp.oscp_co_conf SET
	  modified = ?
	, enabled = ?
	, reg_status = ?
	, cname = ?
	, url = ?
	, oscp_ver = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?