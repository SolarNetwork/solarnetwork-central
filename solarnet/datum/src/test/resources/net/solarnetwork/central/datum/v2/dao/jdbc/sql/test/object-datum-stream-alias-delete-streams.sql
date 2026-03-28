WITH m AS (
	SELECT node_id, source_id, orig_stream_id
	FROM solardatm.da_datm_meta_aliased
)
DELETE FROM solardatm.da_datm_alias da
USING m
WHERE da.node_id = m.node_id
AND da.source_id = m.source_id
AND (
	da.stream_id = ANY(?)
	OR m.orig_stream_id = ANY(?)
)
