SELECT ocp.id, ocp.created, ocp.modified, ocp.user_id, ocp.enabled
	, ocp.fp_id, ocp.reg_status, ocp.cname, ocp.url, ocpt.token, ocp.sprops
FROM solaroscp.oscp_cp_conf ocp
LEFT OUTER JOIN solaroscp.oscp_cp_token ocpt 
	ON ocpt.user_id = ocp.user_id AND ocpt.id = ocp.id
WHERE ocp.user_id = ?
AND ocp.id = ?
ORDER BY ocp.user_id, ocp.id