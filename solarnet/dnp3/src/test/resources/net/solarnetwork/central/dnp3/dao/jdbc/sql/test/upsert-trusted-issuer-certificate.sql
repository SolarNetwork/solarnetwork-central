INSERT INTO solardnp3.dnp3_ca_cert (
	  created,modified,user_id,subject_dn
	, expires,enabled,cert
)
VALUES (
	  ?,?,?,?
	, ?,?,?)
ON CONFLICT (user_id, subject_dn) DO UPDATE
	SET modified = EXCLUDED.modified
		, expires = EXCLUDED.expires
		, enabled = EXCLUDED.enabled
		, cert = EXCLUDED.cert
