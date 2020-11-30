WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s 
	WHERE s.node_id = ANY(?)
)
SELECT (solardatm.calc_datm_at(d, ?)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_around(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
