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
		, to_char({PROPERTY_CALC}, 'FM999999999999999999990.999999999')::NUMERIC AS val
		-- AVG(val)															AS val_avg
		-- SUM(val)															AS val_sum
		-- SUM(CASE prank WHEN 1 THEN val ELSE -val END ORDER BY prank) 	AS val_sub
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
		, d.read_a[p.idx][1] AS rdiff
		, first_value(d.read_a[p.idx][2]) OVER slot AS rstart
		, last_value(d.read_a[p.idx][3]) OVER slot AS rend
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
		, to_char({PROPERTY_CALC}, 'FM999999999999999999990.999999999')::NUMERIC AS val
		-- AVG(val) 															AS val_avg
		-- SUM(val) 															AS val_sum
		-- SUM(CASE prank WHEN 1 THEN val ELSE -val END ORDER BY prank) 		AS val_sub
		, to_char({READING_DIFF_CALC}, 'FM999999999999999999990.999999999')::NUMERIC AS rdiff
		-- AVG(rdiff) 															AS rdiff_avg
		-- SUM(rdiff)															AS rdiff_sum
		-- SUM(CASE prank WHEN 1 THEN rdiff ELSE -rdiff END ORDER BY prank) 	AS rdiff_sub
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
			ARRAY[rdiff, NULL, NULL] ORDER BY pname
		) AS read_a
		, array_agg(pname ORDER BY pname) AS names_a
	FROM da
	GROUP BY stream_id, ts
	ORDER BY stream_id, ts
)
, datum AS (
	SELECT
		  COALESCE(di_ary.stream_id, da_ary.stream_id) AS stream_id
		, COALESCE(di_ary.ts, da_ary.ts) AS ts
		, di_ary.data_i
		, da_ary.data_a
		, NULL::BIGINT[] AS data_s
		, NULL::TEXT[] AS data_t
		, NULL::BIGINT[][] AS stat_i
		, da_ary.read_a
		, di_ary.names_i
		, da_ary.names_a
	FROM di_ary
	FULL OUTER JOIN da_ary ON da_ary.stream_id = di_ary.stream_id AND da_ary.ts = di_ary.ts
)
