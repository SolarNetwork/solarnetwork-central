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
	) RETURNS TABLE(
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
		UNION ALL
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
		UNION ALL
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
		UNION ALL
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
 * Find datm records for an aggregate time range, supporting both "clock" and "reading" spans,
 * including "reset" auxiliary records.
 *
 * The output `rtype` column will be 0, 1, or 2 if the row is a "raw" datum, "final reset" datum,
 * or "starting reset" datum.
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
	) RETURNS TABLE(
		stream_id 	UUID,
		ts 			TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		rtype		SMALLINT
	) LANGUAGE SQL STABLE ROWS 2000 AS
$$
	-- find raw data for given time range
	WITH d AS (
		SELECT d.*, 0::SMALLINT AS rr
		FROM solardatm.find_datm_for_time_span(sid, start_ts, end_ts, tolerance) d
	)
	-- find reset records for same time range, split into two rows for each record: final
	-- and starting accumulating values
	, aux AS (
		SELECT
			  m.stream_id
			, aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, m.names_a
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
			, unnest(ARRAY[1::SMALLINT, 2::SMALLINT]) AS rr
		FROM solardatm.da_datm_aux aux
		INNER JOIN solardatm.da_datm_meta m ON m.stream_id = aux.stream_id
		WHERE aux.atype = 'Reset'::solardatm.da_datm_aux_type
			AND aux.stream_id = sid
			AND aux.ts >= start_ts - tolerance
			AND aux.ts <= end_ts + tolerance
	)
	-- convert reset record rows into datm rows by turning jdata_a JSON into data_a value array,
	-- respecting the array order defined by solardatm.da_datm_meta.names_a and excluding values
	-- not defined there
	, resets AS (
		SELECT
			  aux.stream_id
			, aux.ts
			, NULL::numeric[] AS data_i
			, array_agg(p.val::text::numeric ORDER BY array_position(aux.names_a, p.key::text))
				FILTER (WHERE array_position(aux.names_a, p.key::text) IS NOT NULL)::numeric[] AS data_a
			, NULL::text[] AS data_s
			, NULL::text[] AS data_t
			, min(aux.rr) AS rr
		FROM aux
		INNER JOIN jsonb_each(aux.jdata_a) AS p(key,val) ON TRUE
		GROUP BY aux.stream_id, aux.ts
	)
	-- combine raw datm with reset datm
	, combined AS (
		SELECT * FROM d
		UNION ALL
		SELECT * FROM resets
	)
	-- group all results by time so that reset records with the same time as a raw record
	-- override the raw record
	SELECT DISTINCT ON (ts) stream_id, ts, data_i, data_a, data_s, data_t, rr
	FROM combined
	ORDER BY ts, rr DESC
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
	) RETURNS TABLE(
		stream_id 	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],					-- array of instantaneous property average values
		data_a		NUMERIC[],					-- array of accumulating property clock difference values
		data_s		TEXT[],						-- array of "last seen" status property values
		data_t		TEXT[],						-- array of all tags seen over period
		stat_i		NUMERIC[][],				-- array of instantaneous property [count,min,max] statistic tuples
		read_a		NUMERIC[][]					-- array of accumulating property reading [start,finish,diff] tuples
	) LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- grab raw data + reset records, constrained by stream/date range
	WITH d AS (
		SELECT * FROM solardatm.find_datm_for_time_span_with_aux(
			sid,
			start_ts,
			end_ts,
			tolerance_read
		)
	)
	-- calculate instantaneous values per property
	, wi AS (
		SELECT
			  p.idx AS idx
			, p.val AS val
		FROM d
		INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
			AND d.ts >= start_ts
			AND d.ts < end_ts
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
	-- eliminate excess leading accumulation due to reset records
	, wa_intermediate AS (
		SELECT
			  p.idx AS idx
			, p.val AS val
			, d.ts
			, d.rtype
			, CASE
				WHEN d.ts < start_ts AND lead(d.ts) OVER slot <= start_ts THEN 0
				ELSE 1
				END AS inc
		FROM d
		INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		WINDOW slot AS (PARTITION BY p.idx ORDER BY d.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate clock accumulation for data_a values per property
	, wa AS (
		SELECT
			  p.idx AS idx
			, p.val AS val
			, p.ts
			, p.rtype
			, sum(CASE rtype WHEN 2 THEN 1 ELSE 0 END) OVER slice AS slice
			, CASE rtype
				WHEN 2 THEN 0
				ELSE COALESCE(p.val - lag(p.val) OVER slice, 0::numeric)
				END AS diff
			, first_value(p.val) OVER slot AS rstart
			, CASE
				WHEN row_number() OVER slot = row_number() OVER reading THEN last_value(p.val) OVER reading
				ELSE NULL
				END AS rend
			, CASE
				WHEN p.ts < start_ts
					THEN 0

				WHEN lag(p.ts) OVER slot + tolerance_clock < start_ts
					THEN 0

				WHEN lag(p.ts) OVER slot < start_ts
					THEN  EXTRACT(epoch FROM (p.ts - start_ts)) / EXTRACT(epoch FROM (p.ts - lag(p.ts) OVER slot))

				WHEN p.ts > end_ts + tolerance_clock
					THEN 0

				WHEN p.ts > end_ts AND lag(p.ts) OVER slot >= end_ts
					THEN 0

				WHEN p.ts > end_ts AND lag(p.ts) OVER slot < end_ts
					THEN EXTRACT(epoch FROM (end_ts - lag(p.ts) OVER slot)) / EXTRACT(epoch FROM (p.ts - lag(p.ts) OVER slot))

				ELSE
					1
				END AS portion
		FROM wa_intermediate p
		WHERE p.inc = 1
		WINDOW slot AS (PARTITION BY p.idx ORDER BY p.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
			, slice AS (PARTITION BY p.idx ORDER BY p.ts)
			, reading AS (PARTITION BY p.idx, CASE WHEN p.ts < end_ts THEN 0 ELSE 1 END ORDER BY p.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate accumulating statistics
	, da AS (
		SELECT
			w.idx
			, to_char(sum(w.diff * w.portion) FILTER (WHERE w.portion > 0), 'FM999999999999999999990.999999999')::numeric AS cdiff
			, to_char(sum(w.diff) FILTER (WHERE w.ts < end_ts), 'FM999999999999999999990.999999999')::numeric AS rdiff
			, min(w.rstart) AS rstart
			, min(w.rend) AS rend
		FROM wa w
		GROUP BY w.idx
	)
	-- join data_i and meta_i property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(d.cdiff ORDER BY d.idx) AS data_a
			, array_agg(
				ARRAY[d.rdiff, d.rstart, d.rend] ORDER BY d.idx
			) AS read_a
		FROM da d
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
			  array_agg(d.val ORDER BY d.idx) AS data_s
		FROM ds d
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(p.val ORDER BY d.ts) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL
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
	) RETURNS TABLE(
		stream_id 	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],					-- array of instantaneous property average values
		data_a		NUMERIC[],					-- array of accumulating property clock difference values
		data_s		TEXT[],						-- array of "last seen" status property values
		data_t		TEXT[],						-- array of all tags seen over period
		stat_i		NUMERIC[][],				-- array of instantaneous property [count,min,max] statistic tuples
		read_a		NUMERIC[][]					-- array of accumulating property reading [start,finish,diff] tuples
	) LANGUAGE plpgsql STABLE ROWS 2000 AS
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
	) RETURNS TABLE(
		stream_id 	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],					-- array of instantaneous property average values
		data_a		NUMERIC[],					-- array of accumulating property clock difference values
		data_s		TEXT[],						-- array of "last seen" status property values
		data_t		TEXT[],						-- array of all tags seen over period
		stat_i		NUMERIC[][],				-- array of instantaneous property [count,min,max] statistic tuples
		read_a		NUMERIC[][]					-- array of accumulating property reading [start,finish,diff] tuples
	) LANGUAGE SQL STABLE ROWS 500 AS
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
$$;
