INSERT INTO solardnp3.dnp3_ca_cert (
	  created,modified,user_id,subject_dn
	, expires,enabled,cert
)
VALUES (
	  ?,CAST(COALESCE(?, ?) AS TIMESTAMP WITH TIME ZONE),?,?
	, ?,?,?)
ON CONFLICT (user_id, subject_dn) DO UPDATE
	SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
		, expires = EXCLUDED.expires
		, enabled = EXCLUDED.enabled
		, cert = EXCLUDED.cert
