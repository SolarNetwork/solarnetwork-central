SELECT oco.id, oco.created, oco.modified, oco.user_id, oco.enabled
	, oco.fp_id, oco.reg_status, oco.cname, oco.url, oco.oscp_ver
	, oco.heartbeat_secs, oco.meas_styles, oco.heartbeat_at
	, oco.sprops
FROM solaroscp.oscp_co_conf oco
WHERE oco.user_id = ?
AND oco.id = ?
ORDER BY oco.user_id, oco.id
FOR UPDATE