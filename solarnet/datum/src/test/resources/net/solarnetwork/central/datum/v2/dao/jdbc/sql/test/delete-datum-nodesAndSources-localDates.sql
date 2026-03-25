WITH s AS (
	SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta_aliased s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
	ORDER BY s.stream_id, s.mtype
)
SELECT SUM(d.count)::BIGINT AS count 
FROM s, solardatm.delete_datm(s.stream_id, ?, ?, s.time_zone) d(count)
