SELECT dag.user_id, dag.id, dag.created, dag.modified, dag.enabled
	, dag.allow_public, dag.grantee_id, dag.node_ids, dag.source_ids
FROM solaruser.da_datm_alias_grant dag
WHERE dag.user_id = ?
	AND dag.enabled = ?
ORDER BY dag.user_id, dag.id
