WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta 
	WHERE meta.node_id = ANY(?)
		AND meta.source_id = ANY(?)
)
SELECT (solardatm.calc_datm_at(d, ?)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_around(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
