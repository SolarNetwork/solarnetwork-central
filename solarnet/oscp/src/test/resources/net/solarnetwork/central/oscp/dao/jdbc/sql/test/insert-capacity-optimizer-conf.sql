INSERT INTO solaroscp.oscp_co_conf (
	  created, modified, user_id, enabled, fp_id, reg_status, cname, url
	, pub_in, pub_flux, source_id_tmpl, sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb)