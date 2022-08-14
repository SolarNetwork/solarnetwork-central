INSERT INTO solaruser.user_oscp_cg_conf (
	created,modified,user_id,enabled,cname,ident,meas_secs,cp_id,co_id,sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)