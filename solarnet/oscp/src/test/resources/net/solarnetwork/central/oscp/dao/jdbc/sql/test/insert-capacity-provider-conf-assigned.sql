INSERT INTO solaroscp.oscp_cp_conf (
	created, modified, user_id, id, enabled, fp_id, reg_status, cname, url, sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)