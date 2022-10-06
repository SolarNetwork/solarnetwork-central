UPDATE solaroscp.oscp_co_conf SET
	  modified = ?
	, enabled = ?
	, reg_status = ?
	, cname = ?
	, url = ?
	, oscp_ver = ?
	, pub_in = ?
	, pub_flux = ?
	, source_id_tmpl = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?