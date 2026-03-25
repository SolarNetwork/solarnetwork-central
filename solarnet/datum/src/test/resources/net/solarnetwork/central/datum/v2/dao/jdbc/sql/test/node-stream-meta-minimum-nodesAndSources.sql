SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id
FROM solardatm.da_datm_meta_aliased s
WHERE s.node_id = ANY(?)
	AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
ORDER BY s.stream_id, s.mtype