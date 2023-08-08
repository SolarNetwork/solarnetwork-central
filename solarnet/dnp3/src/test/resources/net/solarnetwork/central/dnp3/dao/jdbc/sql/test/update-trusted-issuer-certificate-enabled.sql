UPDATE solardnp3.dnp3_ca_cert
SET enabled = ?, modified = CURRENT_TIMESTAMP
WHERE user_id = ?
	AND subject_dn = ANY(?)
	AND enabled = ?