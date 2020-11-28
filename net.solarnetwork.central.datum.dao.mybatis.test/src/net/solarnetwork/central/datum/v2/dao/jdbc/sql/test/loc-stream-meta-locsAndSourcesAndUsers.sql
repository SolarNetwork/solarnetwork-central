SELECT meta.stream_id, meta.loc_id, meta.source_id, meta.names_i, meta.names_a, meta.names_s, 
	meta.jdata, 'l'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_loc_datm_meta meta
INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = meta.loc_id
WHERE meta.loc_id = ANY(?)
	AND meta.source_id ~ ANY(ARRAY(
		SELECT r.r
		FROM unnest(?) s(p), solarcommon.ant_pattern_to_regexp(s.p) r(r)
		))
	AND un.user_id = ANY(?)
