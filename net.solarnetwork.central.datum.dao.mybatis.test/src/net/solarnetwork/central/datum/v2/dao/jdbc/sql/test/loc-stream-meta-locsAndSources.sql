SELECT meta.stream_id, meta.loc_id, meta.source_id, meta.names_i, meta.names_a, meta.names_s, 
	meta.jdata, 'l'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_loc_datm_meta meta
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = meta.loc_id
WHERE meta.loc_id = ANY(?)
	AND meta.source_id = ANY(?)
