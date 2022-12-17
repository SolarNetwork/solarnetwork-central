CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_near_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance 		INTERVAL DEFAULT INTERVAL '3 months'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	-- choose earliest first/last rows for start/end dates, which may be exact time matches
	WITH d AS (
		(
		SELECT d.*, 0::SMALLINT AS rtype
		FROM solardatm.find_datm_around(sid, start_ts, tolerance) AS d
		ORDER BY d.ts
		LIMIT 1
		)
		UNION
		(
		SELECT d.*, 0::SMALLINT AS rtype
		FROM solardatm.find_datm_around(sid, end_ts, tolerance) AS d
		ORDER BY d.ts
		LIMIT 1
		)
	)
	, drange AS (
		SELECT
			  COALESCE(min(ts), start_ts) AS ts_min
			, COALESCE(max(ts), end_ts) AS ts_max
		FROM d
	)
	, resets AS (
		SELECT
			  aux.stream_id
			, aux.ts
			, aux.data_a
			, aux.rtype AS rtype
		FROM drange, solardatm.find_datm_aux_for_time_span(
			sid,
			LEAST(drange.ts_min, start_ts),
			GREATEST(drange.ts_max, end_ts)
		) aux
	)
	-- find min, max ts out of raw + resets to eliminate extra leading/trailing from combined results
	, ts_range AS (
		SELECT min_ts, max_ts
		FROM (
				SELECT COALESCE(max(ts), start_ts) AS min_ts
				FROM (
					SELECT max(ts) FROM d WHERE ts <= start_ts
					UNION ALL
					SELECT max(ts) FROM resets WHERE ts <= start_ts
				) l(ts)
			) min, (
				SELECT COALESCE(max(ts), end_ts) AS max_ts
				FROM (
					SELECT max(ts) FROM d WHERE ts <= end_ts
					UNION ALL
					SELECT max(ts) FROM resets WHERE ts <= end_ts
				) r(ts)
			) max
	)
	-- combine raw datm with reset datm
	SELECT d.stream_id
		, d.ts
		, NULL::NUMERIC[] AS data_i
		, d.data_a
		, NULL::TEXT[] AS data_s
		, NULL::TEXT[] AS data_t
		, d.rtype
	FROM d, ts_range
	WHERE d.ts >= ts_range.min_ts AND d.ts <= ts_range.max_ts
	UNION ALL
	SELECT resets.stream_id
		, resets.ts
		, NULL::NUMERIC[] AS data_i
		, resets.data_a
		, NULL::TEXT[] AS data_s
		, NULL::TEXT[] AS data_t
		, resets.rtype
	FROM resets, ts_range
	WHERE resets.ts >= ts_range.min_ts
		-- exclude any reading start record at exactly the end date
		AND (resets.ts < end_ts OR resets.rtype < 2)
$$;


CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	-- we just set a large tolerance for this query
	SELECT * FROM solardatm.find_datm_diff_near_rows(sid, start_ts, end_ts, INTERVAL 'P1Y')
$$;


CREATE OR REPLACE FUNCTION solardatm.find_time_before(
	sid UUID,
	instant TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts < instant
		AND ts >= cutoff
	ORDER BY ts DESC
	LIMIT 1
$$;

CREATE OR REPLACE FUNCTION solardatm.find_time_after(
	sid UUID,
	instant TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts > instant
		AND ts <= cutoff
	ORDER BY ts
	LIMIT 1
$$;

/**
 * Find datm records for an aggregate time range, supporting both "clock" and "reading" spans,
 * including "reset" auxiliary records.
 *
 * The output `rtype` column will be 0, 1, or 2 if the row is a "raw" datum, "final reset" datum,
 * or "starting reset" datum. Reset records for final/start share the same timestamp.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 * @param target_agg		the target aggregate slot, defaults to PT1H
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_for_time_slot(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT INTERVAL 'P3M',
		target_agg 	INTERVAL DEFAULT INTERVAL 'PT1H'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 200 AS
$$
	-- find min/max datum date within slot; if no actual data in this slot we get NULL
	WITH drange AS (
		SELECT (
			-- find minimum datum date within slot
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts < end_ts
			ORDER BY stream_id, ts
			LIMIT 1
		) AS min_ts
		, (
			-- find maximum datum date within slot
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts < end_ts
			ORDER BY stream_id, ts DESC
			LIMIT 1
		) AS max_ts
	)

	-- find prior/next datum date range to provide for clock and reading input
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts) AS min_ts, COALESCE(t.max_ts, drange.max_ts) AS max_ts
		FROM drange, (
			SELECT (
				-- find prior datum date before minimum within slot
				SELECT CASE
					WHEN d.ts IS NULL THEN drange.min_ts
					-- when the prior ts is exactly the start of the proir agg slot, use the datum min instead
					-- because that value would actually be in the previous slot
					WHEN drange.min_ts = start_ts AND d.ts = start_ts - target_agg THEN drange.min_ts
					ELSE d.ts
				END
				FROM drange, solardatm.find_time_before(sid, drange.min_ts, start_ts - tolerance) AS d(ts)
			) AS min_ts
			, (
				-- find next datum date after maximum within slot
				SELECT d.ts
				FROM drange, solardatm.find_time_after(sid, drange.max_ts, end_ts + tolerance)  AS d(ts)
			) AS max_ts
		) t
	)

	-- find date range for resets
	, reset_range AS (
		SELECT MIN(aux.ts) AS min_ts, MAX(aux.ts) AS max_ts
		FROM srange, solardatm.da_datm_aux aux
		WHERE aux.atype = 'Reset'::solardatm.da_datm_aux_type
			AND aux.stream_id = sid
			AND aux.ts >= CASE
					WHEN srange.min_ts <= start_ts
					THEN srange.min_ts
					ELSE start_ts - tolerance
				END
			AND aux.ts <= CASE
				WHEN srange.max_ts >= end_ts
				THEN srange.max_ts
				ELSE end_ts + tolerance
			END
	)

	-- get combined range for datum + resets
	, combined_srange AS (
		SELECT CASE
				-- if datum falls exactly on start, only include prior datum if it is more than
				-- one agg slot away; otherwise that datum will be included in prior slot
				WHEN t.min_ts IS NULL OR (
						drange.min_ts = start_ts
						AND (start_ts - t.min_ts) < target_agg
					)
					THEN drange.min_ts
				ELSE t.min_ts
			END AS min_ts
			, CASE
				WHEN t.max_ts IS NULL OR drange.max_ts = end_ts THEN drange.max_ts
				ELSE t.max_ts
			END AS max_ts
		FROM drange, (
			SELECT CASE
					-- start < reset < datum: reset is min
					WHEN reset_range.min_ts < srange.min_ts
						AND reset_range.min_ts >= start_ts
						THEN reset_range.min_ts

					-- datum < reset < start: reset is min
					WHEN reset_range.min_ts > srange.min_ts
						AND reset_range.min_ts <= start_ts
						THEN reset_range.min_ts

					-- no datum: reset is min
					WHEN srange.min_ts IS NULL THEN reset_range.min_ts

					-- otherwise: datum is min (or null)
					ELSE srange.min_ts
				END AS min_ts
				, CASE
					-- datum < reset < end: reset is max
					WHEN reset_range.max_ts > srange.max_ts
						AND reset_range.max_ts <= end_ts
						THEN reset_range.max_ts

					-- end < reset < datum: reset is max (or null)
					WHEN reset_range.max_ts < srange.max_ts
						AND reset_range.max_ts >= end_ts
						THEN reset_range.max_ts

					-- no datum: reset is max (or null)
					WHEN srange.max_ts IS NULL THEN reset_range.max_ts

					-- otherwise: datum is max (or null)
					ELSE srange.max_ts
				END AS max_ts
			FROM srange, reset_range
		) t
	)

	-- return combined datum + resets
	SELECT d.stream_id
		, d.ts
		, d.data_i
		, d.data_a
		, d.data_s
		, d.data_t
		, 0::SMALLINT AS rtype
	FROM combined_srange, solardatm.da_datm d
	WHERE d.stream_id = sid
		AND d.ts >= combined_srange.min_ts
		AND d.ts <= combined_srange.max_ts

	UNION ALL

	SELECT
		  aux.stream_id
		, aux.ts
		, NULL::numeric[] AS data_i
		, aux.data_a
		, NULL::text[] AS data_s
		, NULL::text[] AS data_t
		, aux.rtype AS rtype
	FROM combined_srange, solardatm.find_datm_aux_for_time_span(
		sid,
		combined_srange.min_ts,
		combined_srange.max_ts
	) aux
$$;


DROP FUNCTION IF EXISTS solardatm.find_datm_for_time_span(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL);

DROP FUNCTION IF EXISTS solardatm.find_datm_for_time_span_with_aux(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL);

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
		FROM m, solardatm.find_datm_for_time_slot(
			sid,
			start_ts,
			end_ts,
			tolerance_read
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
		FROM m, solardatm.find_datm_for_time_slot(
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
