SELECT DISTINCT dag.user_id, s.node_id, s.source_id
FROM solaruser.da_datm_alias_grant dag
INNER JOIN solaruser.user_node un ON un.user_id = dag.user_id
	AND (dag.node_ids IS NULL OR un.node_id = ANY(dag.node_ids))
INNER JOIN solardatm.da_datm_meta s ON s.node_id = un.node_id
	AND (dag.source_regexps IS NULL OR s.source_id ~ ANY(dag.source_regexps))
WHERE dag.grantee_id = ?
	AND dag.enabled = TRUE
	AND dag.user_id = ?
	AND dag.id = ?
	AND solardatm.da_datm_alias_allowed(s.node_id, s.source_id::TEXT, ?)
ORDER BY dag.user_id, s.node_id, s.source_id
