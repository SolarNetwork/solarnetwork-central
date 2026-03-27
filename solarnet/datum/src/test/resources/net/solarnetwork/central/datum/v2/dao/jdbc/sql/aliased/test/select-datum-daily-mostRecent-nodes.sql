WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.node_id = ANY(?)
)
SELECT s.stream_id, 
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
		WHERE datum.stream_id = s.orig_stream_id
		ORDER BY datum.ts_start DESC
		LIMIT 1
	) datum ON datum.stream_id = s.orig_stream_id
ORDER BY stream_id, ts
