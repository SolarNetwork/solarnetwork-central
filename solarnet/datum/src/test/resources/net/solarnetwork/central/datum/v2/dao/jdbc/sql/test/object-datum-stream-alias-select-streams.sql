SELECT da.stream_id, da.created, da.modified
	, da.node_id, da.source_id
	, da.alias_node_id, da.alias_source_id
FROM solardatm.da_datm_alias da
INNER JOIN solardatm.da_datum_meta m
	ON m.node_id = da.node_id
	AND m.source_id = da.source_id
WHERE (
	da.stream_id = ANY(?)
	OR m.stream_id = ANY(?)
)
ORDER BY da.node_id, da.source_id, da.alias_node_id, da.alias_source_id
