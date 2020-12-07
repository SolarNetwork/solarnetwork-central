WITH rs AS (
	SELECT s.stream_id
		, CASE
			WHEN array_position(?, s.node_id) IS NOT NULL THEN ?
			ELSE s.node_id
			END AS node_id
		, COALESCE(array_position(?, s.node_id), 0) AS node_rank
		, CASE
			WHEN array_position(?, s.source_id::TEXT) IS NOT NULL THEN ?
			ELSE s.source_id
			END AS source_id
		, COALESCE(array_position(?, s.source_id::TEXT), 0) AS source_rank
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
, s AS (
	SELECT solardatm.virutal_stream_id(node_id, source_id) AS vstream_id
		, *
	FROM rs
)
, datum AS (
	SELECT s.vstream_id AS stream_id,
		datum.ts_start AS ts,
		(solardatm.rollup_agg_data(
			(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
			ORDER BY datum.ts_start)).*
	FROM s
	INNER JOIN solardatm.rollup_datm_for_time_span_slots(s.stream_id, ?, ?, ?) datum ON datum.stream_id = s.stream_id
	GROUP BY s.vstream_id, ts
)
SELECT datum.*
FROM datum
ORDER BY datum.stream_id, ts