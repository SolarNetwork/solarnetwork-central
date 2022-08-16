INSERT INTO solaroscp.oscp_cp_conf (
	created, modified, user_id, enabled, reg_status, cname, url, sprops
)
VALUES (?,?,?,?,?,?,?,?::jsonb)