UPDATE solardnp3.dnp3_server
SET enabled = ?, modified = CURRENT_TIMESTAMP
WHERE user_id = ?
	AND enabled = ?