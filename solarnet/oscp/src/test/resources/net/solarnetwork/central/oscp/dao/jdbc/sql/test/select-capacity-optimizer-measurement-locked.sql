SELECT c.id, c.created, c.modified, c.user_id, c.enabled
	, c.fp_id, c.reg_status, c.cname, c.url, c.oscp_ver
	, c.heartbeat_secs, c.meas_styles, NULL::timestamptz AS heartbeat_at, c.offline_at
	, c.sprops
	, g.id, g.created, g.modified, g.user_id, g.enabled, g.cname
	, g.ident, g.cp_meas_secs, g.co_meas_secs, g.cp_id, g.co_id, g.sprops
	, NULL::timestamptz AS cp_meas_at, m.meas_at AS co_meas_at
FROM solaroscp.oscp_cg_conf g
INNER JOIN solaroscp.oscp_co_conf c
	ON c.user_id = g.user_id AND c.id = g.co_id
INNER JOIN solaroscp.oscp_asset_conf a
	ON a.user_id = g.user_id AND a.cg_id = g.id AND a.audience = 111
INNER JOIN solaroscp.oscp_cg_co_meas m
	ON m.user_id = g.user_id AND m.cg_id = g.id
WHERE c.reg_status = ascii('r')
	AND c.enabled = TRUE
	AND (m.meas_at IS NULL OR m.meas_at + (g.co_meas_secs * INTERVAL '1 second') < CURRENT_TIMESTAMP)
LIMIT ?
FOR UPDATE OF m SKIP LOCKED