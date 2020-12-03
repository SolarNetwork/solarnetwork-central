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
 * Find the datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_least(
		sid 		UUID,
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY EXECUTE format(
		'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
		'FROM solardatm.%I '
		'WHERE stream_id = $1 ORDER BY ts_start LIMIT 1'
		, CASE kind
			WHEN 'd' THEN 'agg_datm_daily'
			WHEN 'M' THEN 'agg_datm_monthly'
			ELSE 'agg_datm_hourly' END)
	USING sid;
END;
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
 * Find the datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 */
CREATE OR REPLACE FUNCTION solardatm.find_agg_time_greatest(
		sid 		UUID,
		kind 		CHARACTER
	) RETURNS SETOF solardatm.agg_datm LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY EXECUTE format(
		'SELECT stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a '
		'FROM solardatm.%I '
		'WHERE stream_id = $1 ORDER BY ts_start DESC LIMIT 1'
		, CASE kind
			WHEN 'd' THEN 'agg_datm_daily'
			WHEN 'M' THEN 'agg_datm_monthly'
			ELSE 'agg_datm_hourly' END)
	USING sid;
END;
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
 * Find datm records for an aggregate time range, supporting both "clock" and "reading" spans.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_for_time_span(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '3 months'
	) RETURNS TABLE (
		stream_id 	UUID,
		ts 			TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[]
	) LANGUAGE SQL STABLE ROWS 2000 AS
$$
	-- first find boundary datum (least, greatest) for given time range that satisfies both the
	-- clock and reading aggregate time windows
	WITH b AS (
		(
		-- latest on/before start
		SELECT d.stream_id, d.ts
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts <= start_ts
			AND d.ts > start_ts - tolerance
		ORDER BY d.stream_id, d.ts DESC
		LIMIT 1
		)
		UNION
		(
		-- earliest on/after start within range; in case nothing before start
		SELECT d.stream_id, d.ts
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts >= start_ts
			AND d.ts < end_ts
		ORDER BY d.stream_id, d.ts
		LIMIT 1
		)
		UNION
		(
		-- earliest on/after end
		SELECT d.stream_id, d.ts
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts >= end_ts
			AND d.ts < end_ts + tolerance
		ORDER BY d.stream_id, d.ts
		LIMIT 1
		)
		UNION
		(
		-- latest on/before end, in case nothing after end
		SELECT d.stream_id, d.ts
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts <= end_ts
			AND d.ts > start_ts
		ORDER BY d.stream_id, d.ts DESC
		LIMIT 1
		)
	)
	-- combine boundary rows into single range row with start/end columns
	, r AS (
		SELECT
			stream_id
			, COALESCE(min(ts), start_ts) AS range_start
			, COALESCE(max(ts), end_ts) AS range_end
		FROM b
		GROUP BY stream_id
	)
	-- query for raw datum using the boundary range previously found
	SELECT
		  d.stream_id
		, d.ts
		, d.data_i
		, d.data_a
		, d.data_s
		, d.data_t
	FROM r
	INNER JOIN solardatm.da_datm d ON d.stream_id = r.stream_id
	WHERE d.ts >= r.range_start
		AND d.ts <= r.range_end
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
 * @see solardatm.find_datm_for_time_span()
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_for_time_span_with_aux(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '3 months'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 2000 AS
$$
	-- find raw data for given time range
	WITH d AS (
		SELECT d.*, 0::SMALLINT AS rr
		FROM solardatm.find_datm_for_time_span(sid, start_ts, end_ts, tolerance) d
	)
	-- find reset records for same time range, split into two rows for each record: final
	-- and starting accumulating values
	, resets AS (
		SELECT
			  aux.stream_id
			, aux.ts
			, NULL::numeric[] AS data_i
			, aux.data_a
			, NULL::text[] AS data_s
			, NULL::text[] AS data_t
			, aux.rtype AS rr
		FROM solardatm.find_datm_aux_for_time_span(
			sid,
			start_ts - tolerance,
			end_ts + tolerance
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
				SELECT COALESCE(min(ts), end_ts) AS max_ts
				FROM (
					SELECT min(ts) FROM d WHERE ts >= end_ts
					UNION ALL
					SELECT min(ts) FROM resets WHERE ts >= end_ts
				) r(ts)
			) max
	)
	-- combine raw datm with reset datm
	SELECT d.* FROM d, ts_range WHERE d.ts >= ts_range.min_ts AND d.ts <= ts_range.max_ts
	UNION ALL
	SELECT resets.* FROM resets, ts_range WHERE resets.ts >= ts_range.min_ts AND resets.ts <= ts_range.max_ts
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
 * @see solardatm.find_datm_for_time_span_with_aux()
 */
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
	, wi AS (
		SELECT
			  p.idx
			, p.val
		FROM d
		INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL AND d.inc
	)
	-- calculate instantaneous statistics
	, di AS (
		SELECT
			w.idx
			, to_char(avg(w.val), 'FM999999999999999999990.999999999')::numeric AS val
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
	, wa AS (
		SELECT
			  p.idx
			, p.val
			, d.ts
			, d.rtype
			, COALESCE(CASE
				WHEN rtype <> 2 THEN p.val - lag(p.val) OVER slot
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
				OR (ts = end_ts AND rtype = 1)) AS rinc
		FROM d
		INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		WINDOW slot AS (PARTITION BY p.idx ORDER BY d.ts, d.rtype RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			  idx
			, to_char(sum(diff * portion), 'FM999999999999999999990.999999999')::numeric AS cdiff
			, to_char(sum(diff) FILTER (WHERE rinc), 'FM999999999999999999990.999999999')::numeric AS rdiff
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
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  p.idx
			, mode() WITHIN GROUP (ORDER BY p.val) AS val
		FROM d
		INNER JOIN unnest(d.data_s) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL AND d.inc
		GROUP BY p.idx
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
			  array_agg(p.val ORDER BY d.ts) AS data_t
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
 * @see solardatm.find_datm_for_time_span_with_aux()
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
	-- grab raw data + reset records, constrained by stream/date range
	WITH d AS (
		SELECT *, (ts >= start_ts AND ts < end_ts) AS inc
		FROM solardatm.find_datm_for_time_span_with_aux(
			sid,
			start_ts,
			end_ts,
			tolerance_clock
		)
	)
	-- calculate instantaneous statistis per property
	, di AS (
		SELECT
			  p.idx
			, solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs)) AS ts_start
			, to_char(avg(val), 'FM999999999999999999990.999999999')::numeric AS val
			, count(p.val) AS cnt
			, min(p.val) AS val_min
			, max(p.val) AS val_max
		FROM d
		INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL AND d.inc
		GROUP BY p.idx, solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			   ts_start
			,  array_agg(val ORDER BY idx) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
		GROUP BY ts_start
	)
	-- calculate clock accumulation for data_a values per property
	, wa AS (
		SELECT
			  p.idx
			, solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs)) AS ts_start
			, COALESCE(p.val - lag(p.val) OVER slot, 0)::numeric AS diff_before
			, COALESCE(lead(p.val) OVER slot - p.val, 0)::numeric AS diff_after
			, CASE
				-- reset record
				WHEN rtype = 2
					THEN 0

				-- start of slot; allocate only end portion of accumulation within this slot
				WHEN lag(d.ts) OVER slot < solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))
					THEN EXTRACT('epoch' FROM d.ts - solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))) / EXTRACT('epoch' FROM d.ts - lag(d.ts) OVER slot)

				ELSE 1
				END as portion_before
			, CASE
				-- reset record
				WHEN rtype = 2
					THEN 0

				-- end of slot; allocate only start portion of accumulation within this slot
				WHEN solardatm.minute_time_slot(lead(d.ts) OVER slot, solardatm.slot_seconds(secs)) > solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))
					THEN EXTRACT('epoch' FROM solardatm.minute_time_slot(lead(d.ts) OVER slot, solardatm.slot_seconds(secs)) - d.ts) / EXTRACT('epoch' FROM lead(d.ts) OVER slot - d.ts)

				ELSE 0
				END as portion_after
		FROM d
		INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		WINDOW slot AS (PARTITION BY p.idx ORDER BY d.ts, d.rtype RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			  idx
			, ts_start
			, to_char(sum((diff_before * portion_before) + (diff_after * portion_after)), 'FM999999999999999999990.999999999')::numeric AS cdiff
		FROM wa
		GROUP BY idx, ts_start
	)
	-- join accumulating property values back into arrays
	, da_ary AS (
		SELECT
			  ts_start
			, array_agg(cdiff ORDER BY idx) AS data_a
		FROM da
		GROUP BY ts_start
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  p.idx
			, solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs)) AS ts_start
			, mode() WITHIN GROUP (ORDER BY p.val) AS val
		FROM d
		INNER JOIN unnest(d.data_s) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL AND d.inc
		GROUP BY p.idx, solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))
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
			solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs)) AS ts_start
			, array_agg(p.val ORDER BY d.ts) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
		GROUP BY solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs))
	)
	, slots AS (
		SELECT DISTINCT solardatm.minute_time_slot(d.ts, solardatm.slot_seconds(secs)) AS ts_start
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
 * Compute a higher-level rollup datm record of lower=level aggregate datum records over a time range.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param kind 				the aggregate kind: 'h', 'd', or 'M' for daily, hourly, monthly
 * @see solardatm.find_agg_datm_for_time_span()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_agg_datm_for_time_span(
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
			, p.val
			, d.stat_i[p.idx][1] AS cnt
			, d.stat_i[p.idx][2] AS min
			, d.stat_i[p.idx][3] AS max
			, sum(d.stat_i[p.idx][1]) OVER slot AS tot_cnt
		FROM d
		INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		WINDOW slot AS (PARTITION BY p.idx RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate instantaneous statistics
	, di AS (
		SELECT
			idx
			, to_char(sum(val * cnt / tot_cnt), 'FM999999999999999999990.999999999')::numeric AS val
			, sum(cnt) AS cnt
			, min(min) AS val_min
			, max(max) AS val_max
		FROM wi
		GROUP BY idx
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- calculate accumulating values per property
	, wa AS (
		SELECT
			  p.idx
			, p.val
			, first_value(d.read_a[p.idx][1]) OVER slot AS rstart
			, last_value(d.read_a[p.idx][2]) OVER slot AS rend
			, d.read_a[p.idx][3] AS rdiff
		FROM d
		INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		WINDOW slot AS (PARTITION BY p.idx ORDER BY d.ts_start RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			idx
			, to_char(sum(val), 'FM999999999999999999990.999999999')::numeric AS val
			, min(rstart) AS rstart
			, min(rend) AS rend
			, sum(rdiff) AS rdiff
		FROM wa
		GROUP BY idx
	)
	-- join data_a and read_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
			, array_agg(
				ARRAY[rstart, rend, rdiff] ORDER BY idx
			) AS read_a
		FROM da
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  p.idx
			, mode() WITHIN GROUP (ORDER BY p.val) AS val
		FROM d
		INNER JOIN unnest(d.data_s) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		GROUP BY p.idx
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
			  array_agg(p.val ORDER BY d.ts_start) AS data_t
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
