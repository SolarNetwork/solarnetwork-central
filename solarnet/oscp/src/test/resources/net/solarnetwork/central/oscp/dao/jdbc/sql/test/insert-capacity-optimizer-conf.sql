INSERT INTO solaroscp.oscp_co_conf (
	created,modified,user_id,enabled,reg_status,cname,url,token,sprops
)
VALUES (?,?,?,?,?,?,?,?,?::jsonb)