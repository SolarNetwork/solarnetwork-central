WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	WHERE meta.node_id = ANY(?)
		AND meta.source_id ~ ANY(ARRAY(
			SELECT r.r
			FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
			))
)
SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.agg_kind, 
	datum.created
FROM s
INNER JOIN solardatm.agg_stale_datm datum ON datum.stream_id = s.stream_id
WHERE datum.agg_kind = ?
	AND datum.ts_start >= ?
	AND datum.ts_start < ?
ORDER BY datum.agg_kind, ts, datum.stream_id
