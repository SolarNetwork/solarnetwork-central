WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~* ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
SELECT s.stream_id,
	datum.ts_start,
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
		-- the limit is applied to each stream, ordered by time alone so the scan can stop at the
		-- limit under TimescaleDB's ChunkAppend whether the statement is planned with or without
		-- its parameter values; the outer ORDER BY and LIMIT then pick the rows across streams
		AND datum.ts_start >= ?
		AND datum.ts_start < ?
		ORDER BY datum.ts_start
		-- offset + max
		LIMIT ?
	) datum ON datum.stream_id = s.orig_stream_id
ORDER BY ts_start, node_id, source_id
LIMIT ?