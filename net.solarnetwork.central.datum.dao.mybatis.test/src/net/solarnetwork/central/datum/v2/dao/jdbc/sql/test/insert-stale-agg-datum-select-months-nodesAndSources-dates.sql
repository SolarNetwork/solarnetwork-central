INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	WHERE meta.node_id = ANY(?)
		AND meta.source_id ~ ANY(ARRAY(
			SELECT r.r
			FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
			))
)
SELECT datum.stream_id, datum.ts_start, 'M' AS agg_kind
FROM s
INNER JOIN solardatm.find_datm_months(s.stream_id, ?, ?) datum ON TRUE
