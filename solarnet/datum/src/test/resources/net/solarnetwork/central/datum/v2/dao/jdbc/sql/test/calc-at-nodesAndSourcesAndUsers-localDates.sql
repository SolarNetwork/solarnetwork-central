WITH s AS (
	SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta_aliased s 
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
		AND un.user_id = ANY(?)
	ORDER BY s.stream_id, s.mtype
)
SELECT (solardatm.calc_datm_at(d, ? AT TIME ZONE s.time_zone)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_around(s.stream_id, ? AT TIME ZONE s.time_zone, ?) d ON TRUE
GROUP BY s.stream_id
