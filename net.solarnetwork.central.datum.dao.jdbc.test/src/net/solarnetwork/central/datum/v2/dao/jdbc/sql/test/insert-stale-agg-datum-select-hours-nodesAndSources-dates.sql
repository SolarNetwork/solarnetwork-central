INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
SELECT datum.stream_id, datum.ts_start, 'h' AS agg_kind
FROM s
INNER JOIN solardatm.find_datm_hours(s.stream_id, ?, ?) datum ON TRUE
