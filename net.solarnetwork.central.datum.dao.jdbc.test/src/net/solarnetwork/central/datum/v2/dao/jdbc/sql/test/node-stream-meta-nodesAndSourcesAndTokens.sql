SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, 
	s.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta s
INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
INNER JOIN solaruser.user_auth_token ut ON ut.user_id = un.user_id
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE s.node_id = ANY(?)
	AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
	AND ut.auth_token = ANY(?)
	AND (COALESCE(jsonb_array_length(ut.jpolicy->'sourceIds'), 0) < 1
		OR s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(ut.jpolicy->'sourceIds'))))
		)
	AND (COALESCE(jsonb_array_length(ut.jpolicy->'nodeIds'), 0) < 1
		OR s.node_id = ANY(ARRAY(SELECT solarcommon.jsonb_array_to_bigint_array(ut.jpolicy->'nodeIds')))
		)
