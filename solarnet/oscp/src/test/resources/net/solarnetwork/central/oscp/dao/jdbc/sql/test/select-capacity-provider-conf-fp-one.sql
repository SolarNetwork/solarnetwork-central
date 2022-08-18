SELECT ocp.id, ocp.created, ocp.modified, ocp.user_id, ocp.enabled
	, ocp.fp_id, ocp.reg_status, ocp.cname, ocp.url, ocp.oscp_ver, ocp.sprops
FROM solaroscp.oscp_cp_conf ocp
WHERE ocp.user_id = ?
AND ocp.fp_id = ?
ORDER BY ocp.user_id, ocp.id