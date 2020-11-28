INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta meta 
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE meta.node_id = ANY(?)
		AND meta.source_id ~ ANY(ARRAY(
			SELECT r.r
			FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
			))
)
SELECT datum.stream_id, datum.ts_start, 'h' AS agg_kind
FROM s
INNER JOIN solardatm.find_datm_hours(s.stream_id, 
	? AT TIME ZONE s.time_zone, 
	? AT TIME ZONE s.time_zone) datum ON TRUE
