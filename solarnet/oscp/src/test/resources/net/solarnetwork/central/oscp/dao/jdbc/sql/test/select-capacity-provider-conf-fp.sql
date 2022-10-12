SELECT ocp.id, ocp.created, ocp.modified, ocp.user_id, ocp.enabled
	, ocp.fp_id, ocp.reg_status, ocp.cname, ocp.url, ocp.oscp_ver
	, ocp.heartbeat_secs, ocp.meas_styles, ocph.heartbeat_at, ocp.offline_at
	, ocp.sprops
FROM solaroscp.oscp_cp_conf ocp
LEFT OUTER JOIN solaroscp.oscp_cp_heartbeat ocph
	ON ocph.user_id = ocp.user_id AND ocph.id = ocp.id
WHERE ocp.user_id = ANY(?)
AND ocp.fp_id = ANY(?)
ORDER BY ocp.user_id, ocp.id