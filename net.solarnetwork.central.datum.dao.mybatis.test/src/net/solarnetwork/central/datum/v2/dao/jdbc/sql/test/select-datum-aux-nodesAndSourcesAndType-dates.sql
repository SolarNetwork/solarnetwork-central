WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	WHERE meta.node_id = ANY(?)
		AND meta.source_id = ANY(?)
)
SELECT datum.stream_id, 
	datum.ts, 
	datum.atype, 
	datum.updated, 
	datum.notes, 
	datum.jdata_af, 
	datum.jdata_as, 
	datum.jmeta
FROM s
INNER JOIN solardatm.da_datm_aux datum ON datum.stream_id = s.stream_id
WHERE datum.atype = ?::solardatm.da_datm_aux_type
	AND datum.ts >= ?
	AND datum.ts < ?
ORDER BY datum.stream_id, ts, atype
