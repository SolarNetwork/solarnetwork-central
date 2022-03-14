WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
SELECT datum.stream_id, 
	datum.ts, 
	datum.atype, 
	datum.updated, 
	datum.notes, 
	datum.jdata_af, 
	datum.jdata_as, 
	datum.jmeta
FROM s
INNER JOIN solardatm.da_datm_aux datum ON datum.stream_id = s.stream_id
WHERE datum.ts >= ?
	AND datum.ts < ?
ORDER BY datum.stream_id, ts, atype
