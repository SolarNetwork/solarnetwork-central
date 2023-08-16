UPDATE solardnp3.dnp3_server_ctrl
SET enabled = ?, modified = CURRENT_TIMESTAMP
WHERE user_id = ?
	AND server_id = ?
	AND idx = ANY(?)
	AND enabled = ?