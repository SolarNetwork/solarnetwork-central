WITH rs AS (
	SELECT s.stream_id
		, CASE
			WHEN array_position(?, s.node_id) IS NOT NULL THEN ?
			ELSE s.node_id
			END AS node_id
		, COALESCE(array_position(?, s.node_id), 0) AS node_rank
		, CASE
			WHEN array_position(?, s.source_id) IS NOT NULL THEN ?
			ELSE s.source_id
			END AS source_id
		, COALESCE(array_position(?, s.source_id), 0) AS source_rank
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
, s AS (
	SELECT uuid_generate_v5(uuid_ns_url(), 'objid://obj/' || node_id || '/' || TRIM(LEADING '/' FROM source_id)) AS vstream_id
	, *
	FROM rs
)
SELECT s.vstream_id AS stream_id,
	datum.ts_start AS ts,
	(solardatm.rollup_agg_data(
		(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
		ORDER BY datum.ts_start)).*
FROM s
INNER JOIN solardatm.agg_datm_daily datum ON datum.stream_id = s.stream_id
WHERE datum.ts_start >= ?
	AND datum.ts_start < ?
GROUP BY s.vstream_id, ts
ORDER BY s.vstream_id, ts
