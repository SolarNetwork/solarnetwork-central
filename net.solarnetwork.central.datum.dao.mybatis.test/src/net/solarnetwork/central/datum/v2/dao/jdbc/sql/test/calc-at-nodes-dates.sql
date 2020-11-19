WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a
		, meta.names_s, meta.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta meta 
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE meta.node_id = ANY(?)
)
SELECT (solardatm.calc_datm_at(d, ?)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_around(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
