CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT interval '1 hour',
		tolerance_read 	INTERVAL DEFAULT interval '3 months'
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- grab raw data + reset records, constrained by stream/date range
	WITH d AS (
		SELECT *, (ts >= start_ts AND ts < end_ts) AS inc
		FROM solardatm.find_datm_for_time_span_with_aux(
			sid,
			start_ts,
			end_ts,
			tolerance_read
		)
	)
	-- calculate instantaneous values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, wi AS (
		SELECT
			  p.idx
			, d.data_i[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_i, 1)) AS p(idx) ON TRUE
		WHERE d.inc
	)
	-- calculate instantaneous statistics
	, di AS (
		SELECT
			w.idx
			, to_char(avg(w.val), 'FM999999999999999999999999999999999999990.999999999')::numeric AS val
			, count(w.val) AS cnt
			, min(val) AS val_min
			, max(val) AS val_max
		FROM wi w
		GROUP BY w.idx
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			  array_agg(d.val ORDER BY d.idx) AS data_i
			, array_agg(
				ARRAY[d.cnt, d.val_min, d.val_max] ORDER BY d.idx
			) AS stat_i
		FROM di d
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
			, to_char(sum(diff * portion), 'FM999999999999999999999999999999999999990.999999999')::numeric AS cdiff
			, to_char(sum(diff) FILTER (WHERE rinc), 'FM999999999999999999999999999999999999990.999999999')::numeric AS rdiff
			, solarcommon.first(val ORDER BY ts, rtype) FILTER (WHERE rinc) AS rstart
			, solarcommon.first(val ORDER BY ts DESC, rtype DESC) FILTER (WHERE rinc) AS rend
		FROM wa
		GROUP BY idx
	)
	-- join accumulating property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(cdiff ORDER BY idx) AS data_a
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
		WHERE d.inc
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
