UPDATE solaroscp.oscp_cg_conf SET
	  modified = ?
	, enabled = ?
	, cname = ?
	, ident = ?
	, cp_meas_secs = ?
	, co_meas_secs = ?
	, cp_id = ?
	, co_id = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?