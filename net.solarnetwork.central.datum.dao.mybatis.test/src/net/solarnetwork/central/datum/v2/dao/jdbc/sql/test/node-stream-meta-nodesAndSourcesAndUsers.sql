SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a, meta.names_s, 
	meta.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta meta
INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE meta.node_id = ANY(?)
	AND meta.source_id = ANY(?)
	AND un.user_id = ANY(?)
