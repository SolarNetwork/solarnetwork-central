UPDATE solaroscp.oscp_co_conf SET
	  modified = ?
	, heartbeat_secs = ?
	, meas_styles = ?
WHERE user_id = ? AND id = ?