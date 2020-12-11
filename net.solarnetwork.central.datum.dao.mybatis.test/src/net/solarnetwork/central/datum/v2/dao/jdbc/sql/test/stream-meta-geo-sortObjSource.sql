	SELECT s.stream_id, s.loc_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)

SELECT m.stream_id, obj_id, source_id, names_i, names_a, names_s, jdata, 'l'::CHARACTER AS kind, time_zone
FROM unnest(?) s(stream_id)
INNER JOIN solardatm.find_metadata_for_stream(s.stream_id) m ON TRUE
ORDER BY obj_id, source_id
