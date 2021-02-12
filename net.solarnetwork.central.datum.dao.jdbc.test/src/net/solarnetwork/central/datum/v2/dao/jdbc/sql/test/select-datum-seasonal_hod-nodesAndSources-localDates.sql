WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?) 
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
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
INNER JOIN solardatm.find_agg_datm_hod_seasonal(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone) datum ON datum.stream_id = s.stream_id
ORDER BY datum.stream_id, ts