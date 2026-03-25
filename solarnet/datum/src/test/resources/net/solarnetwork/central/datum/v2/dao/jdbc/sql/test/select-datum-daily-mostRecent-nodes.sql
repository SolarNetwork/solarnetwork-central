WITH s AS (
	SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.node_id = ANY(?)
	ORDER BY s.stream_id, s.mtype
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
