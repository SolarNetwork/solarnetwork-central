WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, s.orig_stream_id
	FROM solardatm.da_datm_meta_aliased s 
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
, datum AS (
	SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*
		, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
	FROM s
	INNER JOIN solardatm.find_datm_diff_rows(s.orig_stream_id, ?, ?) d ON TRUE
	GROUP BY s.orig_stream_id
)
SELECT s.stream_id
	, datum.ts_start
	, datum.ts_end
	, datum.data_i
	, datum.data_a
	, datum.data_s
	, datum.data_t
	, datum.stat_i
	, datum.read_a
FROM s
INNER JOIN datum ON datum.stream_id = s.orig_stream_id
