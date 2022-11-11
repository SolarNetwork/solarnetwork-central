SELECT c.id, c.created, c.modified, c.user_id, c.enabled
	, c.fp_id, c.reg_status, c.cname, c.url, c.oscp_ver
	, c.heartbeat_secs, c.meas_styles, h.heartbeat_at, c.offline_at
	, c.sprops
FROM solaroscp.oscp_co_conf c
INNER JOIN solaroscp.oscp_co_heartbeat h
	ON c.user_id = h.user_id AND c.id = h.id
WHERE c.reg_status = ascii('r')
	AND c.heartbeat_secs > 0
	AND (h.heartbeat_at IS NULL OR heartbeat_at
		+ (c.heartbeat_secs * INTERVAL '1 second') < CURRENT_TIMESTAMP)
	AND c.enabled = TRUE
LIMIT ?
FOR UPDATE OF h SKIP LOCKED