UPDATE solardnp3.dnp3_server_auth
SET enabled = ?, modified = CURRENT_TIMESTAMP
WHERE user_id = ?
	AND server_id = ?
	AND ident = ANY(?)
	AND enabled = ?