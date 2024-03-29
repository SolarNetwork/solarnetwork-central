WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
SELECT datum.stream_id,
	date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,
	(solardatm.rollup_agg_data(
		(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
		ORDER BY datum.ts_start)).*
FROM s
INNER JOIN solardatm.agg_datm_daily datum ON datum.stream_id = s.stream_id
WHERE datum.ts_start >= ?
	AND datum.ts_start < ?
GROUP BY datum.stream_id, date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone
ORDER BY datum.stream_id, ts
