SELECT oco.id, oco.created, oco.modified, oco.user_id, oco.enabled
	, oco.fp_id, oco.reg_status, oco.cname, oco.url, oco.oscp_ver
	, oco.heartbeat_secs, oco.meas_styles, ocoh.heartbeat_at, oco.offline_at
	, oco.sprops
	, oco.pub_in, oco.pub_flux, oco.source_id_tmpl
FROM solaroscp.oscp_co_conf oco
LEFT OUTER JOIN solaroscp.oscp_co_heartbeat ocoh
	ON ocoh.user_id = oco.user_id AND ocoh.id = oco.id
WHERE oco.user_id = ?
AND oco.id = ?
ORDER BY oco.user_id, oco.id
FOR UPDATE OF oco