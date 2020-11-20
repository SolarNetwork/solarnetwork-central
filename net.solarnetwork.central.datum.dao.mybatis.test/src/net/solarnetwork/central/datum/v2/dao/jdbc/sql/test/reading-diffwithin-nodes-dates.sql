WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta 
	WHERE meta.node_id = ANY(?)
)
SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_diff_within_rows(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
