SELECT s.stream_id, s.loc_id, s.source_id, s.names_i, s.names_a, s.names_s, 
	s.jdata, 'l'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_loc_datm_meta s
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = s.loc_id
WHERE s.loc_id = ANY(?)
	AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
