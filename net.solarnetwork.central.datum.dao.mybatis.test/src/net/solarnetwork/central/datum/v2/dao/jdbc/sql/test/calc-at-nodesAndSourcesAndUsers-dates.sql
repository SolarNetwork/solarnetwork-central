WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta 
	INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
	WHERE meta.node_id = ANY(?)
		AND meta.source_id ~ ANY(ARRAY(
			SELECT r.r
			FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
			))
		AND un.user_id = ANY(?)
)
SELECT (solardatm.calc_datm_at(d, ?)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_around(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
