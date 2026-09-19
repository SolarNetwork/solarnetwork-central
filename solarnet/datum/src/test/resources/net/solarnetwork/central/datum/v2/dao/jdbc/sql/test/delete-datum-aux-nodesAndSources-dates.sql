WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
DELETE FROM solardatm.da_datm_aux datum
USING s
WHERE datum.stream_id = s.stream_id
	AND datum.atype = ?::solardatm.da_datm_aux_type
	AND datum.ts >= ?
	AND datum.ts < ?
