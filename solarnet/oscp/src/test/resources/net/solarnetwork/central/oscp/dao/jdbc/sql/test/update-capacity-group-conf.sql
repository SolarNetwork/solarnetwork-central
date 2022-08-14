UPDATE solaruser.user_oscp_cg_conf SET
	  modified = ?
	, enabled = ?
	, cname = ?
	, ident = ?
	, meas_secs = ?
	, cp_id = ?
	, co_id = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?