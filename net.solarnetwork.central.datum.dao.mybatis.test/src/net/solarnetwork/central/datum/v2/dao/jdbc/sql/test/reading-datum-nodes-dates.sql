WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a, meta.names_s, meta.jdata, l.time_zone
	FROM solardatm.da_datm_meta meta 
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE meta.node_id = ANY(?)
) SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*
FROM s
INNER JOIN solardatm.find_datm_diff_rows(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
