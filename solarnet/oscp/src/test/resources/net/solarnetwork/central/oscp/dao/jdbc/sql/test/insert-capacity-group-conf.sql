INSERT INTO solaroscp.oscp_cg_conf (
	created,modified,user_id,enabled,cname,ident,cp_meas_secs,co_meas_secs,cp_id,co_id,sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?,?::jsonb)