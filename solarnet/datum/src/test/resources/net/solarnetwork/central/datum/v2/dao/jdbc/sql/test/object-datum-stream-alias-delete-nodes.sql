DELETE FROM solardatm.da_datm_alias da
WHERE (
	da.alias_node_id = ANY(?)
	OR da.node_id = ANY(?)
)
