SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, 
	s.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta s
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id 
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id 
WHERE s.names_a @> ?
