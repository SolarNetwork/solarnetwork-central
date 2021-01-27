SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s,
	s.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta s
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
INNER JOIN LATERAL (
	SELECT stream_id
	FROM solardatm.da_datm datum
	WHERE datum.stream_id = s.stream_id
		AND datum.ts >= ?
		AND datum.ts < ?
	LIMIT 1) d ON d.stream_id = s.stream_id
WHERE s.node_id = ANY(?)
