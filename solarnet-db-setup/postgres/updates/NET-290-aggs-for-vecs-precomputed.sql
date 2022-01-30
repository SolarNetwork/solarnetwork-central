CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT interval '1 hour',
		tolerance_read 	INTERVAL DEFAULT interval '3 months'
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			 , COALESCE(array_length(names_a, 1), 0) AS len_a
		FROM solardatm.da_datm_meta
		WHERE stream_id = sid
	)
	-- grab raw data + reset records, constrained by stream/date range
	, d AS (
		SELECT
			  stream_id
			, ts
			, pad_vec(data_i, len_i) AS data_i
			, pad_vec(data_a, len_a) AS data_a
			, data_s
			, data_t
			, rtype
			, (ts >= start_ts AND ts < end_ts) AS inc
		FROM m, solardatm.find_datm_for_time_span_with_aux(
			sid,
			start_ts,
			end_ts,
			tolerance_clock
		)
	)
	-- calculate instantaneous statistis per property
	, di_ary AS (
		SELECT
			  vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarcommon.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::numeric[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
	)
	-- calculate clock accumulation for data_a values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.ts
			, d.rtype
			, COALESCE(CASE
				WHEN rtype <> 2 THEN d.data_a[p.idx] - lag(d.data_a[p.idx]) OVER slot
				ELSE 0::numeric
				END, 0)::numeric AS diff
			, CASE
				-- too much time between rows for clock diff, or no time passed
				WHEN d.ts - lag(d.ts) OVER slot > tolerance_clock OR d.ts = lag(d.ts) OVER slot
					THEN 0

				-- before bounds
				WHEN d.ts < start_ts
					THEN 0

				-- crossing start of slot; allocate only end portion of accumulation within this slot
				WHEN lag(d.ts) OVER slot < start_ts
					THEN EXTRACT('epoch' FROM d.ts - start_ts) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				-- crossing end of slot; allocate only start portion of accumulation within this slot
				WHEN d.ts > end_ts AND lag(d.ts) OVER slot < end_ts
					THEN EXTRACT('epoch' FROM end_ts - lag(d.ts) OVER slot) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				ELSE 1
				END as portion
			, (ts < end_ts AND NOT (ts <= start_ts AND rtype = 1)
				OR (ts = end_ts AND rtype < 2)) AS rinc
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts, d.rtype
			RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			  idx
			, sum(diff * portion::numeric) AS cdiff
			, sum(diff) FILTER (WHERE rinc) AS rdiff
			, solarcommon.first(val ORDER BY ts, rtype) FILTER (WHERE rinc) AS rstart
			, solarcommon.first(val ORDER BY ts DESC, rtype DESC) FILTER (WHERE rinc) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join accumulating property values back into arrays
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(cdiff ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- calculate status for data_s values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, ws AS (
		SELECT
			  p.idx AS idx
			, d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	-- join data_s property values back into arrays
	, ds_ary AS (
		SELECT
			  array_agg(d.val ORDER BY d.idx) AS data_s
		FROM ds d
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
	)
	SELECT
		sid AS stream_id
		, start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;

CREATE OR REPLACE FUNCTION solardatm.rollup_agg_data_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		kind 			CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	WITH d AS (
		SELECT * FROM solardatm.find_agg_datm_for_time_span(
			sid,
			start_ts,
			end_ts,
			kind
		)
	)
	-- calculate instantaneous values per property
	, wi AS (
		SELECT
			  p.idx
			, d.data_i[p.idx] AS val
			, d.stat_i[p.idx][1] AS cnt
			, d.stat_i[p.idx][2] AS min
			, d.stat_i[p.idx][3] AS max
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_i, 1)) AS p(idx) ON TRUE
	)
	-- calculate instantaneous statistics
	, di AS (
		SELECT
			idx
			, sum(val * cnt) AS val
			, sum(cnt) AS cnt
			, min(min) AS val_min
			, max(max) AS val_max
		FROM wi
		GROUP BY idx
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val / cnt ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- calculate accumulating values per property
	, wa AS (
		SELECT
			  p.idx
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, first_value(d.read_a[p.idx][2]) OVER slot_start AS rstart
			, first_value(d.read_a[p.idx][3]) OVER slot_end AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
		WINDOW slot_start AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			slot_end AS (PARTITION BY p.idx ORDER BY CASE WHEN d.data_a[p.idx] IS NULL THEN 1 ELSE 0 END, d.ts_start DESC
				RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, min(rstart) AS rstart
			, min(rend) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[rdiff, rstart, rend] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- calculate status for data_s values per property
	, ws AS (
		SELECT
			  p.idx AS idx,
			  d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  ws.idx
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx
	)
	-- join data_s property values back into arrays
	, ds_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_s
		FROM ds
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL
	)
	SELECT
		sid AS stream_id,
		start_ts AS ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;

-- remove function that duplicates rollup_agg_data_for_time_span() and is unused
DROP FUNCTION IF EXISTS solardatm.combine_agg_data_for_time_span(
	UUID, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE, CHARACTER);

CREATE OR REPLACE FUNCTION solardatm.rollup_agg_data_ffunc(agg_state solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	WITH di AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	, da AS (
		SELECT
			  p.idx
			, p.val
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
		FROM da
	)
	SELECT
		  di_ary.data_i
		, da_ary.data_a
		, agg_state.data_s
		, agg_state.data_t
		, di_ary.stat_i
		, agg_state.read_a
	FROM di_ary, da_ary
	INTO agg_state;

	return agg_state;
END;
$$;

CREATE OR REPLACE FUNCTION solardatm.avg_agg_data_ffunc(agg_state solardatm.agg_data)
RETURNS solardatm.agg_data LANGUAGE plpgsql STRICT IMMUTABLE AS
$$
BEGIN
	WITH di AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[1] AS cnt
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_i) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.stat_i) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, di_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	, da AS (
		SELECT
			  p.idx
			, p.val / s.stat[1] AS val
			, s.stat[2] AS val_min
			, s.stat[3] AS val_max
		FROM unnest(agg_state.data_a) WITH ORDINALITY AS p(val, idx)
		INNER JOIN solarcommon.reduce_dim(agg_state.read_a) WITH ORDINALITY AS s(stat, idx) ON s.idx = p.idx
	)
	, da_ary AS (
		SELECT
			  vec_trim_scale(array_agg(val ORDER BY idx)) AS data_a
			, array_agg(
				ARRAY[NULL, val_min, val_max] ORDER BY idx
			) AS read_a
		FROM da
	)
	SELECT
		  di_ary.data_i
		, da_ary.data_a
		, agg_state.data_s
		, agg_state.data_t
		, di_ary.stat_i
		, da_ary.read_a
	FROM di_ary, da_ary
	INTO agg_state;

	return agg_state;
END;
$$;
