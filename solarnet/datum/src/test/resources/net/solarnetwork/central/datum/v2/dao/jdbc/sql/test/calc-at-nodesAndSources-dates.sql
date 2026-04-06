WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s 
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
SELECT (solardatm.calc_datm_at(d, ?)).*
FROM s, solardatm.find_datm_around(s.stream_id, ?, ?) d
GROUP BY d.stream_id
