INSERT INTO solaruser.user_oscp_cp_conf (
	created,modified,user_id,enabled,reg_status,cname,url,token,sprops
)
VALUES (?,?,?,?,?,?,?,?,?::jsonb)