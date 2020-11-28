SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a, meta.names_s, 
	meta.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta meta
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id 
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id 
WHERE meta.node_id = ANY(?)
	AND meta.source_id ~ ANY(ARRAY(
		SELECT r.r
		FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
		))
