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
		, COALESCE(array_position(?, s.source_id::TEXT), 0) AS source_rank, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
)
, s AS (
	SELECT solardatm.virutal_stream_id(node_id, source_id) AS vstream_id
		, *
	FROM rs
)
, vs AS (
	SELECT DISTINCT ON (vstream_id) vstream_id, node_id, source_id
	FROM s	
)
, datum AS (
	-- leading partial agg
	SELECT s.vstream_id AS stream_id,
		date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,
		(solardatm.rollup_agg_data(
			(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
			ORDER BY datum.ts_start)).*
	FROM s
	INNER JOIN solardatm.agg_datm_monthly datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < ? AT TIME ZONE s.time_zone
	GROUP BY s.vstream_id, date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone
	HAVING COUNT(*) > 0
	
	-- middle main agg (
	UNION ALL
	SELECT s.vstream_id AS stream_id,
		date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,
		(solardatm.rollup_agg_data(
			(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
			ORDER BY datum.ts_start)).*
	FROM s
	INNER JOIN solardatm.agg_datm_monthly datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < ? AT TIME ZONE s.time_zone
	GROUP BY s.vstream_id, date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone
	HAVING COUNT(*) > 0
	
	-- trailing partial agg	
	UNION ALL
	SELECT s.vstream_id AS stream_id,
		date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,
		(solardatm.rollup_agg_data(
			(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
			ORDER BY datum.ts_start)).*
	FROM s
	INNER JOIN solardatm.agg_datm_monthly datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < ? AT TIME ZONE s.time_zone
	GROUP BY s.vstream_id, date_trunc('year', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone
	HAVING COUNT(*) > 0
)
SELECT datum.*
FROM datum
INNER JOIN vs ON vs.vstream_id = datum.stream_id
ORDER BY ts, node_id, source_id
