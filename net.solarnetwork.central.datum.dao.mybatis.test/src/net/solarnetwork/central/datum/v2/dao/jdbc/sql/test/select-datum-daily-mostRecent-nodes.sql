WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	WHERE meta.node_id = ANY(?)
)
SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.data_i, 
	datum.data_a, 
	datum.data_s, 
	datum.data_t,
	datum.stat_i,
	datum.read_a 
FROM s
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.agg_datm_daily datum
		WHERE datum.stream_id = s.stream_id
		ORDER BY datum.ts_start DESC
		LIMIT 1
	) datum ON datum.stream_id = s.stream_id
ORDER BY datum.stream_id, ts
