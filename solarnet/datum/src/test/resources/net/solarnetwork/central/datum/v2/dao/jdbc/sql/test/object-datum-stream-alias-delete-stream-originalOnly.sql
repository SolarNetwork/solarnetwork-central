DELETE FROM solardatm.da_datm_alias da
USING solardatm.da_datm_meta m
WHERE da.node_id = m.node_id
AND da.source_id = m.source_id
AND m.stream_id = ?
