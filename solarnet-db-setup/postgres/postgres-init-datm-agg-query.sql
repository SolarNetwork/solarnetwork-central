/*
 NOTE ON CLOCK VS READING

 "Clock" refers to normalized clock periods, where datum rows are interpolated at exact period
 start and end value. This affects accumulating properties in that difference of accumulation
 between two rows on either side of a period transition is split proportionally to each period.

 "Reading" refers to periods where the datum included range from the latest row on or before the
 period start to the latest row before the period end. No interpolation is applied between the
 resulting rows within that range.

 Unless otherwise noted, these functions assume that "reading" tolerance values are larger than
 "clock" tolerance values. The assumption stems from the idea that the "clock" aggregates are
 designed for charting purposes while "reading" for billing.
*/

/**
 * Find the hourly datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least_hourly(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_hourly
	WHERE stream_id = sid
	ORDER BY ts_start
	LIMIT 1
$$;


/**
 * Find the daily datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least_daily(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_daily
	WHERE stream_id = sid
	ORDER BY ts_start
	LIMIT 1
$$;


/**
 * Find the monthly datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least_monthly(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_monthly
	WHERE stream_id = sid
	ORDER BY ts_start
	LIMIT 1
$$;


/**
 * Find the datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * The supported `kind` values are: 0, h, d, M.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least(
		sid 		UUID,
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE plpgsql STABLE STRICT ROWS 1 AS
$$
BEGIN
	CASE kind
		WHEN '0' THEN
			RETURN QUERY SELECT stream_id, ts AS ts_start, data_i, data_a, data_s, data_t
					, NULL::NUMERIC[][] AS stat_i, NULL::NUMERIC[][] AS read_a
				FROM solardatm.find_time_least(sid);

		WHEN 'd' THEN
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_least_daily(sid);

		WHEN 'M' THEN
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_least_monthly(sid);

		ELSE
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_least_hourly(sid);
	END CASE;
END
$$;


/**
 * Find the datum with the smallest timestamp for a set of streams, i.e. the "first" datum in each
 * stream.
 *
 * @param sids the stream IDs to search for
 * @see solardatm.find_agg_time_least(uuid)
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least(
		sids 		UUID[],
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE AS
$$
	SELECT d.*
	FROM unnest(sids) ids(stream_id)
	INNER JOIN solardatm.find_agg_time_least(ids.stream_id, kind) d ON TRUE
$$;


/**
 * Find the hourly datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest_hourly(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_hourly
	WHERE stream_id = sid
	ORDER BY ts_start DESC
	LIMIT 1
$$;


/**
 * Find the daily datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest_daily(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_daily
	WHERE stream_id = sid
	ORDER BY ts_start DESC
	LIMIT 1
$$;


/**
 * Find the monthly datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest_monthly(
		sid 		UUID
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE STRICT ROWS 1 AS
$$
	SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a
	FROM solardatm.agg_datm_monthly
	WHERE stream_id = sid
	ORDER BY ts_start DESC
	LIMIT 1
$$;


/**
 * Find the datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * The supported `kind` values are: 0, h, d, M.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest(
		sid 		UUID,
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE plpgsql STABLE STRICT ROWS 1 AS
$$
BEGIN
	CASE kind
		WHEN '0' THEN
			RETURN QUERY SELECT stream_id, ts AS ts_start, data_i, data_a, data_s, data_t
					, NULL::NUMERIC[][] AS stat_i, NULL::NUMERIC[][] AS read_a
				FROM solardatm.find_time_greatest(sid);

		WHEN 'd' THEN
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_greatest_daily(sid);

		WHEN 'M' THEN
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_greatest_monthly(sid);

		ELSE
			RETURN QUERY SELECT * FROM solardatm.find_agg_time_greatest_hourly(sid);
	END CASE;
END
$$;


/**
 * Find the datum with the largest timestamp for a set of streams, i.e. the "last" datum in each
 * stream.
 *
 * @param sids the stream IDs to search for
 * @see solardatm.find_agg_time_greatest(uuid)
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest(
		sids 		UUID[],
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE SQL STABLE AS
$$
	SELECT d.*
	FROM unnest(sids) ids(stream_id)
	INNER JOIN solardatm.find_agg_time_greatest(ids.stream_id, kind) d ON TRUE
$$;


/**
 * Find aggregate datm records for a time range.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_datm_for_time_span(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE plpgsql STABLE ROWS 2000 AS
$$
BEGIN
	RETURN QUERY EXECUTE format(
		'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
		'FROM solardatm.%I '
		'WHERE stream_id = $1 AND ts_start >= $2 AND ts_start < $3'
		, 'agg_datm_' || CASE kind WHEN 'd' THEN 'daily' WHEN 'M' THEN 'monthly' ELSE 'hourly' END)
	USING sid, start_ts, end_ts;
END;
$$;


/**
 * Find datm auxiliary records for a time range.
 *
 * The output `rtype` column will be 1, or 2 if the row is a "final reset" datum
 * or "starting reset" datum. Reset records for final/start share the same timestamp.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (inclusive)
 * @param aux_type			the auxiliary type to look for
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_aux_for_time_span(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		aux_type 	solardatm.da_datm_aux_type DEFAULT 'Reset'::solardatm.da_datm_aux_type
	) RETURNS TABLE (
		stream_id 	UUID,
		ts 			TIMESTAMP WITH TIME ZONE,
		data_a		NUMERIC[],
		rtype		SMALLINT
	) LANGUAGE SQL STABLE ROWS 50 AS
$$
	-- find reset records for same time range, split into two rows for each record: final
	-- and starting accumulating values
	WITH aux AS (
		SELECT
			  m.stream_id
			, aux.ts
			, m.names_a
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
			, unnest(ARRAY[1::SMALLINT, 2::SMALLINT]) AS rr
		FROM solardatm.da_datm_aux aux
		INNER JOIN solardatm.da_datm_meta m ON m.stream_id = aux.stream_id
		WHERE aux.atype = aux_type
			AND aux.stream_id = sid
			AND aux.ts >= start_ts
			AND aux.ts <= end_ts
	)
	-- convert reset record rows into datm rows by turning jdata_a JSON into data_a value array,
	-- respecting the array order defined by solardatm.da_datm_meta.names_a and excluding values
	-- not defined there
	SELECT
		  aux.stream_id
		, aux.ts
		, array_agg(p.val::text::numeric ORDER BY array_position(aux.names_a, p.key::text))
			FILTER (WHERE array_position(aux.names_a, p.key::text) IS NOT NULL)::numeric[] AS data_a
		, min(aux.rr) AS rtype
	FROM aux
	INNER JOIN jsonb_each(aux.jdata_a) AS p(key,val) ON TRUE
	GROUP BY aux.stream_id, aux.ts, aux.rr
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


/**
 * Compute a rollup datm record for a time range, including "reset" auxiliary records.
 *
 * The `data_i` output column contains the average values for the raw `data_i` properties within
 * the "clock" period.
 *
 * The `stat_i` output column contains a tuple of [count, min, max] values for the raw `data_i`
 * properties within the "clock" period.
 *
 * The `data_a` output column contains the accumulated difference of the raw `data_a` properties
 * within the "clock" period.
 *
 * The `read_a` output column contains a tuple of [start, finish, difference] values for the raw
 * `data_a` properties within the "reading" period.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance_clock 	the maximum time to look forward/backward for adjacent datm within
 *                          the "clock" period
 * @param tolerance_read 	the maximum time to look forward/backward for adjacent datm within
 *                          the "reading" period
 * @see solardatm.find_datm_for_time_slot()
 */
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


/**
 * Calculate the number of rollup datum would be returned for a time range and slot period.
 *
 * This is meant to compliment the `solardatm.rollup_datm_for_time_span_slots()` function to
 * more quickly find the count of datum slots that are available. This function can be much
 * faster to execute when only a count is needed.
 *
 * @param sid 				the stream ID to find the count of datm slots for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param secs				the slot period, in seconds; must be between 60 and 1800 and evenly
 *                          divide into 1800
 */
CREATE OR REPLACE FUNCTION solardatm.count_datm_time_span_slots(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		secs			INTEGER DEFAULT 600
	) RETURNS BIGINT LANGUAGE SQL STABLE AS
$$
	SELECT count(*) FROM (
		SELECT DISTINCT solardatm.minute_time_slot(ts, solardatm.slot_seconds(secs))
		FROM solardatm.da_datm
		WHERE stream_id = sid
			AND ts >= start_ts
			AND ts < end_ts
	) slots
$$;


/**
 * Compute a rollup datm record for a time range into sub-hour "slots", including "reset" auxiliary records.
 *
 * The `data_i` output column contains the average values for the raw `data_i` properties within
 * the "clock" period.
 *
 * The `stat_i` output column contains a tuple of [count, min, max] values for the raw `data_i`
 * properties within the "clock" period.
 *
 * The `data_a` output column contains the accumulated difference of the raw `data_a` properties
 * within the "clock" period.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param secs				the slot period, in seconds; must be between 60 and 1800 and evenly
 *                          divide into 1800
 * @param tolerance_clock 	the maximum time to look forward/backward for adjacent datm within
 *                          the "clock" period
 *
 * @see solardatm.count_datm_time_span_slots()
 * @see solardatm.find_datm_for_time_slot()
 * @see solardatm.slot_seconds()
 */
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


/**
 * Compute a higher-level rollup datm record of lower=level aggregate datum records over a time range.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 * @see solardatm.find_agg_datm_for_time_span()
 */
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
