WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a
		, meta.names_s, meta.jdata, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta meta
	INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE meta.node_id = ANY(?) 
		AND meta.source_id = ANY(?) 
		AND un.user_id = ANY(?)
)
SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).* 
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s 
INNER JOIN solardatm.find_datm_diff_rows(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone) d ON TRUE
GROUP BY s.stream_id
