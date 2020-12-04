WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
, obj_mappings AS (
	SELECT id, vid, ROW_NUMBER() OVER (ORDER BY 1) AS rank FROM (
		SELECT unnest(ids) AS id, v.vid, s.n FROM (
			SELECT ids, n
			FROM solarcommon.reduce_dim(?) WITH ORDINALITY AS m(ids,n)
		) s
	JOIN unnest(?) WITH ORDINALITY AS v(vid,n) ON v.n = s.n
	) os
)
, source_mappings AS (
	SELECT id, vid, ROW_NUMBER() OVER (ORDER BY 1) AS rank FROM (
		SELECT unnest(ids) AS id, v.vid, s.n FROM (
			SELECT ids, n
			FROM solarcommon.reduce_dim(?) WITH ORDINALITY AS m(ids,n)
		) s
	JOIN unnest(?) WITH ORDINALITY AS v(vid,n) ON v.n = s.n
	) os
)
SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.data_i, 
	datum.data_a, 
	datum.data_s, 
	datum.data_t,
	datum.stat_i,
	datum.read_a 
FROM s
INNER JOIN solardatm.agg_datm_daily datum ON datum.stream_id = s.stream_id
WHERE datum.ts_start >= ?
	AND datum.ts_start < ?
ORDER BY datum.stream_id, ts
