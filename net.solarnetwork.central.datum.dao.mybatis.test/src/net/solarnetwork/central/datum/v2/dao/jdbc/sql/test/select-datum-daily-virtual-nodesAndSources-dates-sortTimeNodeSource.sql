WITH rs AS (
	SELECT s.stream_id
		, CASE
			WHEN array_position(?, s.node_id) IS NOT NULL THEN ?
			ELSE s.node_id
			END AS node_id
		, COALESCE(array_position(?, s.node_id), 0) AS obj_rank
		, CASE
			WHEN array_position(?, s.source_id::TEXT) IS NOT NULL THEN ?
			ELSE s.source_id
			END AS source_id
		, COALESCE(array_position(?, s.source_id::TEXT), 0) AS source_rank
		, s.names_i
		, s.names_a
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
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
, d AS (
	SELECT s.vstream_id AS stream_id,
		s.obj_rank,
		s.source_rank,
		s.names_i,
		s.names_a,
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
)
-- calculate instantaneous values per date + property NAME (to support joining different streams with different index orders)
-- ordered by object/source ranking defined by query metadata; assume names are unique per stream
, wi AS (
	SELECT
		  d.stream_id
		, d.ts
		, p.val
		, rank() OVER slot as prank
		, d.names_i[p.idx] AS pname
		, d.stat_i[p.idx][1] AS cnt
		, SUM(d.stat_i[p.idx][1]) OVER slot AS tot_cnt
	FROM d
	INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
	WHERE p.val IS NOT NULL
	WINDOW slot AS (PARTITION BY d.stream_id, d.ts, d.names_i[p.idx] ORDER BY d.obj_rank, d.source_rank RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY d.stream_id, d.ts, d.names_i[p.idx], d.obj_rank, d.source_rank
)
-- calculate instantaneous statistics
, di AS (
	SELECT
		  stream_id
		, ts
		, pname
		, to_char(SUM(val), 'FM999999999999999999990.999999999')::NUMERIC AS val
		, SUM(cnt) AS cnt
	FROM wi
	GROUP BY stream_id, ts, pname
	ORDER BY stream_id, ts, pname
)
-- join property data back into arrays; no stat_i for virtual stream
, di_ary AS (
	SELECT
		  stream_id
		, ts
		, array_agg(val ORDER BY pname) AS data_i
		, array_agg(pname ORDER BY pname) AS names_i
	FROM di
	GROUP BY stream_id, ts
	ORDER BY stream_id, ts
)
-- calculate accumulating values per date + property NAME (to support joining different streams with different index orders)
-- ordered by object/source ranking defined by query metadata; assume names are unique per stream
, wa AS (
	SELECT
		  d.stream_id
		, d.ts
		, p.val
		, rank() OVER slot as prank
		, d.names_a[p.idx] AS pname 
		, first_value(d.read_a[p.idx][1]) OVER slot AS rstart
		, last_value(d.read_a[p.idx][2]) OVER slot AS rend
		, d.read_a[p.idx][3] AS rdiff
	FROM d
	INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
	WHERE p.val IS NOT NULL
	WINDOW slot AS (PARTITION BY d.stream_id, d.ts, d.names_a[p.idx] ORDER BY d.obj_rank, d.source_rank RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY d.stream_id, d.ts, d.names_a[p.idx], d.obj_rank, d.source_rank
)
-- calculate accumulating statistics
, da AS (
	SELECT
		  stream_id
		, ts
		, pname
		, to_char(SUM(val), 'FM999999999999999999990.999999999')::NUMERIC AS val
		, to_char(SUM(rdiff), 'FM999999999999999999990.999999999')::NUMERIC AS rdiff
	FROM wa
	GROUP BY stream_id, ts, pname
	ORDER BY stream_id, ts, pname
)
-- join property data back into arrays; only read_a.diff for virtual stream
, da_ary AS (
	SELECT
		  stream_id
		, ts
		, array_agg(val ORDER BY pname) AS data_a
		, array_agg(
			ARRAY[NULL, NULL, rdiff] ORDER BY pname
		) AS read_a
		, array_agg(pname ORDER BY pname) AS names_a
	FROM da
	GROUP BY stream_id, ts
	ORDER BY stream_id, ts
)
, datum AS (
	SELECT
		  vs.vstream_id AS stream_id
		, COALESCE(di_ary.ts, da_ary.ts) AS ts
		, solarcommon.first(di_ary.data_i) AS data_i
		, solarcommon.first(da_ary.data_a) AS data_a
		, NULL::BIGINT[] AS data_s
		, NULL::TEXT[] AS data_t
		, NULL::BIGINT[][] AS stat_i
		, solarcommon.first(da_ary.read_a) AS read_a
		, solarcommon.first(di_ary.names_i) AS names_i
		, solarcommon.first(da_ary.names_a) AS names_a
	FROM vs, di_ary, da_ary
	GROUP BY vs.vstream_id, COALESCE(di_ary.ts, da_ary.ts)
)
SELECT datum.*
FROM datum
INNER JOIN vs ON vs.vstream_id = datum.stream_id
ORDER BY ts, node_id, source_id