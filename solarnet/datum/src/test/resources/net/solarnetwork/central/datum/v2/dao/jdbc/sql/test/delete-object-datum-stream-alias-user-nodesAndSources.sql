WITH m AS (
	SELECT node_id, source_id
	FROM solaruser.da_datm_meta_aliased
	WHERE user_id = ?
	AND node_id = ANY(?)
)
DELETE FROM solardatm.da_datm_alias da
USING m
WHERE da.node_id = m.node_id
AND da.source_id = m.source_id
 AND (
	da.alias_node_id = ANY(?)
	OR da.node_id = ANY(?)
)
AND (
	da.alias_source_id = ANY(?)
	OR da.source_id = ANY(?)
)
