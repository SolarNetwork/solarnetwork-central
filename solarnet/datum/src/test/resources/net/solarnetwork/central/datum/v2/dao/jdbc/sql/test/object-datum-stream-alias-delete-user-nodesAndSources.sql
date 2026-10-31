DELETE FROM solardatm.da_datm_alias da
USING solaruser.user_node un
WHERE da.alias_node_id = un.node_id
AND un.user_id = ?
AND (
	da.alias_node_id = ANY(?)
	OR da.node_id = ANY(?)
)
AND (
	da.alias_source_id = ANY(?)
	OR da.source_id = ANY(?)
)
