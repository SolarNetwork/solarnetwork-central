WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id = ANY(?)
)
SELECT datum.stream_id,
	datum.ts,
	datum.received,
	datum.data_i,
	datum.data_a,
	datum.data_s,
	datum.data_t
FROM s
INNER JOIN solardatm.da_datm datum ON datum.stream_id = s.stream_id
WHERE datum.ts >= ?
	AND datum.ts < ?
ORDER BY stream_id, ts DESC
LIMIT ?