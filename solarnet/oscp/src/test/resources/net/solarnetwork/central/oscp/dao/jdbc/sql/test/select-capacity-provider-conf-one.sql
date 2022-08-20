SELECT ocp.id, ocp.created, ocp.modified, ocp.user_id, ocp.enabled
	, ocp.fp_id, ocp.reg_status, ocp.cname, ocp.url, ocp.oscp_ver
	, ocp.heartbeat_secs, ocp.meas_styles, ocp.heartbeat_at, ocp.offline_at
	, ocp.sprops
FROM solaroscp.oscp_cp_conf ocp
WHERE ocp.user_id = ?
AND ocp.id = ?
ORDER BY ocp.user_id, ocp.id