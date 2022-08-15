SELECT ocp.id,ocp.created,ocp.modified,ocp.user_id,ocp.enabled
	,ocp.reg_status,ocp.cname,ocp.url,ocp.token,ocp.sprops
FROM solaroscp.oscp_cp_conf ocp
WHERE ocp.user_id = ?
AND ocp.id = ?
ORDER BY ocp.user_id,ocp.id