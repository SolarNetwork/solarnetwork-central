WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.node_id = ?
		AND s.source_id ~* solarcommon.ant_pattern_to_regexp(?)
)
SELECT s.stream_id,
	datum.ts,
	datum.received,
	datum.data_i,
	datum.data_a,
	datum.data_s,
	datum.data_t
FROM s
INNER JOIN solardatm.da_datm datum ON datum.stream_id = s.orig_stream_id
WHERE datum.ts >= ?
	AND datum.ts < ?
ORDER BY stream_id, ts DESC
LIMIT ?