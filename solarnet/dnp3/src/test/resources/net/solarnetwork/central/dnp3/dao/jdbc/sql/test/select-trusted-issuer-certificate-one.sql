SELECT dcc.subject_dn, dcc.created, dcc.modified, dcc.user_id
	, dcc.expires, dcc.enabled, dcc.cert
FROM solardnp3.dnp3_ca_cert dcc
WHERE dcc.user_id = ?
	AND dcc.subject_dn = ?
	AND dcc.enabled = ?
ORDER BY dcc.user_id, dcc.subject_dn