INSERT INTO solaroscp.oscp_co_conf (
	created, modified, user_id, enabled, fp_id, reg_status, cname, url, sprops
)
VALUES (?,?,?,?,?,?,?,?,?::jsonb)