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
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.da_datm datum
		WHERE datum.stream_id = s.stream_id
		-- the limit is applied to each stream, ordered by time alone so the scan can stop at the
		-- limit under TimescaleDB's ChunkAppend whether the statement is planned with or without
		-- its parameter values; the outer ORDER BY and LIMIT then pick the rows across streams
		AND datum.ts >= ?
		AND datum.ts < ?
		ORDER BY datum.ts DESC
		-- offset + max
		LIMIT ?
	) datum ON datum.stream_id = s.stream_id
ORDER BY ts DESC, node_id, source_id
LIMIT ? OFFSET ?