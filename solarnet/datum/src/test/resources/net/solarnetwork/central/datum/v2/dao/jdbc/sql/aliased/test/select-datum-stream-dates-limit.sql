WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.stream_id = ?
)
SELECT s.stream_id,
	datum.ts,
	datum.received,
	datum.data_i,
	datum.data_a,
	datum.data_s,
	datum.data_t
FROM s
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.da_datm datum
		WHERE datum.stream_id = s.orig_stream_id
		-- the limit is applied to each stream, ordered by time alone so the scan can stop at the
		-- limit under TimescaleDB's ChunkAppend whether the statement is planned with or without
		-- its parameter values; the outer ORDER BY and LIMIT then pick the rows across streams
		AND datum.ts >= ?
		AND datum.ts < ?
		ORDER BY datum.ts
		-- offset + max
		LIMIT ?
	) datum ON datum.stream_id = s.orig_stream_id
ORDER BY stream_id, ts
LIMIT ?