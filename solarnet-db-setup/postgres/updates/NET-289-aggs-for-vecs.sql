CREATE OR REPLACE FUNCTION solarcommon.array_transpose2d(arr anyarray)
	RETURNS anyarray LANGUAGE SQL IMMUTABLE AS
$$
    SELECT array_agg(
		(SELECT array_agg(arr[i][j] ORDER BY i) FROM generate_subscripts(arr, 1) AS i)
		ORDER BY j
	)
    FROM generate_subscripts(arr, 2) as j
$$;

CREATE OR REPLACE FUNCTION solardatm.count_datm_time_span_slots(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600
	) RETURNS BIGINT LANGUAGE SQL STABLE AS
$$
	SELECT count(*) FROM (
		SELECT DISTINCT time_bucket((secs||' seconds')::interval, ts)
		FROM solardatm.da_datm
		WHERE stream_id = sid
			AND ts >= start_ts
			AND ts < end_ts
	) slots
$$;

CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span_slots(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600,
		tolerance_clock INTERVAL DEFAULT interval '1 hour'
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- grab array column lenghts for pad_vec() and fill_array() later
	WITH m AS (
		SELECT COALESCE(array_length(names_i, 1), 0) AS len_i
			 , COALESCE(array_length(names_a, 1), 0) AS len_a
		FROM solardatm.find_metadata_for_stream(sid)
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
			  time_bucket((secs||' seconds')::interval, d.ts) AS ts_start
			, vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarcommon.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::numeric[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		GROUP BY time_bucket((secs||' seconds')::interval, d.ts)
	)
	-- calculate clock accumulation for data_a values per property
	, wa AS (
		SELECT
			 time_bucket((secs||' seconds')::interval, d.ts) AS ts_start
			, d.data_a
			, COALESCE(vec_sub(d.data_a, lag(d.data_a) OVER win), array_fill(0, ARRAY[m.len_a])) AS diff_before
			, COALESCE(vec_sub(lead(d.data_a) OVER win, d.data_a), array_fill(0, ARRAY[m.len_a])) AS diff_after
			, CASE
				-- reset record
				WHEN rtype = 2
					THEN 0

				-- start of slot; allocate only end portion of accumulation within this slot
				WHEN rank() OVER slot = 1
					THEN COALESCE(EXTRACT('epoch' FROM d.ts - time_bucket((secs||' seconds')::interval, d.ts)) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER win), 0)

				ELSE 1
				END as portion_before
			, CASE
				-- reset record
				WHEN rtype = 2
					THEN 0

				-- end of slot; allocate only start portion of accumulation within this slot
				WHEN rank() OVER rslot = 1
					THEN COALESCE(EXTRACT('epoch' FROM time_bucket((secs||' seconds')::interval, lead(d.ts) OVER win) - d.ts) / EXTRACT('epoch' FROM lead(d.ts) OVER win - d.ts), 0)

				ELSE 0
				END as portion_after
		FROM d, m
		WINDOW slot AS (PARTITION BY time_bucket((secs||' seconds')::interval, d.ts) ORDER BY CASE WHEN d.data_a IS NULL THEN 1 ELSE 0 END, d.ts, d.rtype
						RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
			, rslot AS (PARTITION BY time_bucket((secs||' seconds')::interval, d.ts) ORDER BY CASE WHEN d.data_a IS NULL THEN 1 ELSE 0 END DESC, d.ts DESC, d.rtype DESC
						RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
			, win AS (ORDER BY CASE WHEN d.data_a IS NULL THEN 1 ELSE 0 END, d.ts, d.rtype
						RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da_ary AS (
		SELECT
			  ts_start
			, vec_trim_scale(vec_to_sum(vec_add(vec_mul(diff_before, portion_before::numeric), vec_mul(diff_after, portion_after::numeric)))) AS data_a
		FROM wa
		GROUP BY ts_start
	)
	-- calculate status for data_s values per property
	-- NOTE "unnest() WITH ORDINALITY" not used because of possible sparse array slice
	, ws AS (
		SELECT
			  p.idx AS idx
			, d.data_s[p.idx] AS val
			, time_bucket((secs||' seconds')::interval, d.ts) AS ts_start
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.inc
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  ws.idx
			, ws.ts_start
			, mode() WITHIN GROUP (ORDER BY ws.val) AS val
		FROM ws
		GROUP BY ws.idx, ws.ts_start
	)
	-- join data_s property values back into arrays
	, ds_ary AS (
		SELECT
			  ts_start
			, array_agg(val ORDER BY idx) AS data_s
		FROM ds
		GROUP BY ts_start
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			time_bucket((secs||' seconds')::interval, d.ts) AS ts_start
			, array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
		GROUP BY time_bucket((secs||' seconds')::interval, d.ts)
	)
	, slots AS (
		SELECT DISTINCT time_bucket((secs||' seconds')::interval, d.ts) AS ts_start
		FROM d
		WHERE inc
	)
	SELECT
		  sid AS stream_id
		, slots.ts_start
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
		, NULL::NUMERIC[][] AS read_a
	FROM slots
	LEFT OUTER JOIN di_ary ON di_ary.ts_start = slots.ts_start
	LEFT OUTER JOIN da_ary ON da_ary.ts_start = slots.ts_start
	LEFT OUTER JOIN ds_ary ON ds_ary.ts_start = slots.ts_start
	LEFT OUTER JOIN dt_ary ON dt_ary.ts_start = slots.ts_start
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;

