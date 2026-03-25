WITH s AS (
	SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta_aliased s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
	ORDER BY s.stream_id, s.mtype
)
SELECT SUM(datum.dcount) AS dcount
FROM s
-- stream_id, start, end, secs
INNER JOIN solardatm.count_datm_time_span_slots(s.stream_id, ?, ?, ?) datum(dcount) ON TRUE
