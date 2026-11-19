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
		, array_agg(val::TEXT::NUMERIC ORDER BY a_idx) AS data_a
		, min(aux.rr) AS rtype
	FROM aux
	INNER JOIN unnest(aux.names_a) WITH ORDINALITY AS a(a_name, a_idx) ON TRUE
	LEFT OUTER JOIN jsonb_each(aux.jdata_a) AS p(key,val) ON p.key = a.a_name
	GROUP BY aux.stream_id, aux.ts, aux.rr
$$;


/**
 * Find datm records for an aggregate time range, supporting both "clock" and "reading" spans,
 * including "reset" auxiliary records.
 *
 * The output `rtype` column will be 0, 1, or 2 if the row is a "raw" datum, "final reset" datum,
 * or "starting reset" datum. Reset records for final/start share the same timestamp.
 *
 * The datum before the slot is first searched for, with accumulating properties, within the 14
 * days before the slot (or the tolerance, if shorter). If none is found there, the latest datum
 * before those 14 days and within the tolerance is used if it has accumulating properties.
 * Otherwise the latest datum with accumulating properties within the 14 days before it, and
 * within the tolerance, is used.
 *
 * The datum before the slot is searched for only when something returned can be differenced
 * against it: a datum with accumulating properties in the slot (counting one exactly at `end_ts`,
 * which is returned for the reading that ends the slot), a reset record in the slot (counting one
 * exactly at `start_ts` or `end_ts`), or the next datum with accumulating properties after the
 * slot. Otherwise only the datum in the slot are returned, as when the search finds nothing.
 *
 * This function is not declared STRICT, so that PostgreSQL can inline it into the calling query
 * and plan it with the argument values given, using only the chunks that can match. It returns
 * no rows if any argument is NULL, as a STRICT function would.
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
	-- find if stream even has accumulating properties, to avoid costly scan
	WITH meta AS (
		SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
 		FROM solardatm.find_metadata_for_stream(sid)
	)
	-- Find min/max datum date within slot; if no actual data in this slot we get a
	-- result row with NULL values.
	-- Note that an INCLUSIVE end date it used to pick up a "gap" slot that could occur
	-- leading up to the end time slot (see NET-469).
	, drange AS (
		SELECT *
		FROM (
			-- force a result row
			VALUES (TRUE)
		) AS c(t)
		LEFT OUTER JOIN (
			-- find minimum datum date within slot
			SELECT ts, COALESCE(CARDINALITY(data_a) > 0, FALSE)
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY ts
			LIMIT 1
		) AS l(min_ts, min_has_a) ON TRUE
		LEFT OUTER JOIN (
			-- find maximum datum date within slot
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY ts DESC
			LIMIT 1
		) AS r(max_ts) ON TRUE
	)

	-- find next datum date after maximum within slot (or exact end of slot) REQUIRING accumulating;
	-- the next datum only contributes to the clock accumulation that crosses the end of the slot,
	-- which is ignored beyond the 1 hour clock tolerance, so limiting the search to that keeps
	-- REQUIRING accumulating fast even when no such datum exists (see NET-537)
	, nrange AS (
		SELECT (
			SELECT CASE
				WHEN d.ts IS NULL THEN drange.max_ts
				WHEN drange.max_ts = end_ts THEN drange.max_ts
				ELSE d.ts
			END
			FROM meta, drange, solardatm.find_time_after_ts(sid, drange.max_ts, end_ts + LEAST(tolerance, INTERVAL 'PT1H'), TRUE, meta.has_no_a) AS d(ts)
		) AS max_ts
	)

	-- decide if the prior datum is needed, which is only when something returned can be
	-- differenced against it; otherwise its search is skipped, as that reads every row back to
	-- the datum it finds, or its whole window when there is none (see NET-539)
	, prior AS (
		-- a stream without accumulating properties has no prior datum to find
		SELECT NOT COALESCE((SELECT has_no_a FROM meta), TRUE) AND (
				-- the first datum in the slot has accumulating properties, which decides it for
				-- most streams without another lookup
				COALESCE(drange.min_has_a, FALSE)

				-- the next datum after the slot is returned, so the clock accumulation from the
				-- prior datum to it crosses the slot
				OR COALESCE(nrange.max_ts > drange.max_ts, FALSE)

				-- any datum in the slot has accumulating properties, counting one exactly at the
				-- end of the slot (see NET-469)
				OR EXISTS (
					SELECT 1
					FROM solardatm.da_datm d
					WHERE d.stream_id = sid
						AND d.ts >= start_ts
						AND d.ts <= end_ts
						AND d.data_a IS NOT NULL
				)

				-- a reset record is in the slot, counting one exactly at its start or end: its final
				-- reading is differenced against the prior datum, which also limits the search for
				-- reset records in reset_range below
				OR EXISTS (
					SELECT 1
					FROM solardatm.da_datm_aux aux
					WHERE aux.atype = 'Reset'::solardatm.da_datm_aux_type
						AND aux.stream_id = sid
						AND aux.ts >= start_ts
						AND aux.ts <= end_ts
				)
			) AS needed
		FROM drange, nrange
	)

	-- find prior/next datum date range to provide for clock and reading input
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts, start_ts) AS min_ts, COALESCE(nrange.max_ts, drange.max_ts, end_ts) AS max_ts
		FROM drange, nrange, (
			-- search for the prior datum only when it is needed; not searching gives the same
			-- result as a search that finds nothing (see NET-539)
			SELECT CASE WHEN prior.needed THEN COALESCE(
				(
					-- find prior datum date before minimum within slot (or exact start of slot) REQUIRING accumulating
					-- but use forced shorter tolerance because REQUIRING accumulating too expensive
					SELECT CASE
						WHEN d.ts IS NULL THEN drange.min_ts
						WHEN drange.min_ts = start_ts AND drange.min_has_a THEN drange.min_ts
						ELSE d.ts
					END
					FROM meta, drange, solardatm.find_time_before_ts(sid, COALESCE(drange.min_ts, start_ts), start_ts - LEAST(tolerance, INTERVAL 'P14D'), TRUE, meta.has_no_a) AS d(ts)
				),
				(
					-- expand search past 14d slow search:
					-- find prior datum date before minimum within slot (or exact start of slot) NOT REQUIRING accumulating
					-- using full tolerance because index-only scan possible and thus fast enough
					WITH way_back AS (
						SELECT CASE
							WHEN d.ts IS NULL THEN drange.min_ts
							WHEN drange.min_ts = start_ts AND drange.min_has_a THEN drange.min_ts
							ELSE d.ts
						END
						FROM meta, drange, solardatm.find_time_before_ts(
							  sid
							, CASE WHEN drange.min_has_a THEN drange.min_ts ELSE start_ts END - LEAST(tolerance, INTERVAL 'P14D')
							, start_ts - tolerance
							, FALSE
							, meta.has_no_a
							) AS d(ts)
					)
					-- find datum with way_back time, to see if has accumulating properties
					, way_back_d AS (
						SELECT d.ts, d.has_a
						FROM way_back, solardatm.datm_has_accumulating_at(sid, way_back.ts) AS d
					)
					SELECT COALESCE(
						(
							-- if way-back datum has accumulating properties, use it
							SELECT ts
							FROM way_back_d
							WHERE has_a
						),
						(
							-- otherwise try short slow search before way-back datum for accumulating
							-- properties, within the tolerance, unless stream has none (see NET-538)
							SELECT d.ts
							FROM meta, way_back_d, solardatm.find_time_before_ts(
								  sid
								, way_back_d.ts
								, GREATEST(way_back_d.ts - LEAST(tolerance, INTERVAL 'P14D'), start_ts - tolerance)
								, TRUE
								, meta.has_no_a
							) AS d(ts)
							WHERE NOT meta.has_no_a
						)
					)
				)
			) END AS min_ts
			FROM prior
		) t
	)

	-- find date range for resets
	, reset_range AS (
		SELECT MIN(aux.ts) AS min_ts, MAX(aux.ts) AS max_ts
		FROM srange, solardatm.da_datm_aux aux
		WHERE aux.atype = 'Reset'::solardatm.da_datm_aux_type
			AND aux.stream_id = sid
			AND aux.ts >= CASE
					WHEN srange.min_ts <= start_ts THEN srange.min_ts
					ELSE start_ts - tolerance
				END
			AND aux.ts <= CASE
				WHEN srange.max_ts >= end_ts THEN srange.max_ts
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
						AND drange.min_has_a
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

					-- no datum but reset: reset is min
					WHEN drange.min_ts IS NULL
						AND reset_range.min_ts IS NOT NULL
						THEN LEAST(srange.min_ts, reset_range.min_ts)

					-- no datum
					WHEN drange.min_ts IS NULL THEN start_ts

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

					-- no datum but reset: reset is max (or null)
					WHEN drange.max_ts IS NULL
						AND reset_range.max_ts IS NOT NULL
						THEN LEAST(srange.max_ts, reset_range.max_ts)

					-- no datum
					WHEN drange.max_ts IS NULL THEN end_ts

					-- otherwise: datum is max (or null)
					ELSE srange.max_ts
				END AS max_ts
			FROM drange, srange, reset_range
		) t
		-- a NULL argument gives no rows, as for a STRICT function (see NET-538)
		WHERE num_nulls(sid, start_ts, end_ts, tolerance, target_agg) = 0
	)

	-- return combined datum + resets
	SELECT d.stream_id
		, d.ts
		, d.data_i
		, d.data_a
		, d.data_s
		, d.data_t
		, 0::SMALLINT AS rtype
	FROM combined_srange, solardatm.find_datm_between(sid, combined_srange.min_ts, combined_srange.max_ts) d

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
 * The `read_a` output column contains a tuple of [difference, start, end] values for the raw
 * `data_a` properties within the "reading" period.
 *
 * NOTE `solardatm.rollup_datm_15m_for_hour()` holds a copy of this function's query, extended
 * to compute the 15 minute rows of the hour as well: a change here must be made there too. The
 * two are kept apart because the extended query costs more, and this one is still used for the
 * hours before the 15 minute "complete from" boundary.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance_clock 	the maximum time to look forward/backward for adjacent datm within
 *                          the "clock" period
 * @param tolerance_read 	the maximum time to look forward/backward for adjacent datm within
 *                          the "reading" period
 * @see solardatm.find_datm_for_time_slot()
 * @see solardatm.rollup_datm_15m_for_hour()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_datm_for_time_span(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
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

				-- both rows at or after the end of the slot: the difference belongs to a later slot
				-- (two reset records after the end, before the next datum; see NET-540)
				WHEN lag(d.ts) OVER slot >= end_ts
					THEN 0

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
 * Get the time from which the 15 minute aggregate table `solardatm.agg_datm_15m` is complete.
 *
 * Hours that start at or after this time get their 15 minute rows when they are processed; hours
 * that start before it get none.
 *
 * @return the time, `-infinity` if the table is complete for all time, or `NULL` if no hour gets
 *         15 minute rows
 * @see solardatm.agg_datm_15m_setting
 */
CREATE OR REPLACE FUNCTION solardatm.agg_datm_15m_complete_from()
	RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE AS
$$
	SELECT complete_from FROM solardatm.agg_datm_15m_setting
$$;


/**
 * Compute the rollup datm record of one hour, together with the rollup datm records of the 15
 * minute slots of that hour.
 *
 * The first eight output columns are the hour, exactly as `solardatm.rollup_datm_for_time_span()`
 * computes it for the same arguments, and no row is returned when that function returns none. The
 * `slots` column holds the hour's 15 minute rows in time order, or `NULL` when there are none.
 *
 * The slots are computed from exactly the rows, and the per-row differences and portions, that
 * the hour uses, and each contribution to the hour is given to one or two of its slots, so the
 * slots add up to the hour:
 *
 *  - clock accumulation between two rows goes to the slot of the earlier row, up to the start of
 *    the slot of the later row; the later row's slot gets the remainder of the hour's portion
 *  - a contribution that crosses the start of the hour goes to the slot of the later row, and one
 *    that crosses the end of the hour to the slot of the earlier row
 *  - a reading difference goes to the slot of its row; a row before the hour counts in the first
 *    slot holding a row, and a row exactly at the end of the hour in the last one (or the last
 *    slot of the hour, if no slot holds a row)
 *  - averages, counts, minimums, maximums, status and tags are grouped by slot
 *  - a slot whose rows have no value for an accumulating property gets NULL for it when another
 *    slot of the hour has one, and zero when none has, as the hour reports
 *
 * Slots are 15 minute buckets of UTC time, so the hour must start on a 15 minute boundary, which
 * every hour of a whole, half or quarter hour time zone does.
 *
 * NOTE the CTEs that compute the hour are a copy of the query of
 * `solardatm.rollup_datm_for_time_span()`, with only `p_ts` added to `wa`: a change there must be
 * made here too. Sharing them would make every caller of that function pay for the slots.
 *
 * @param sid 				the stream ID to find datm for
 * @param start_ts			the start of the hour (inclusive)
 * @param end_ts 			the end of the hour (exclusive)
 * @param tolerance_clock 	as for `solardatm.rollup_datm_for_time_span()`
 * @param tolerance_read 	as for `solardatm.rollup_datm_for_time_span()`
 * @see solardatm.rollup_datm_for_time_span()
 * @see solardatm.process_one_agg_stale_datm()
 */
CREATE OR REPLACE FUNCTION solardatm.rollup_datm_15m_for_hour(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance_clock INTERVAL DEFAULT INTERVAL 'PT1H',
		tolerance_read 	INTERVAL DEFAULT INTERVAL 'P1Y'
	) RETURNS TABLE (
		stream_id	UUID,
		ts_start	TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		stat_i		NUMERIC[][],
		read_a		NUMERIC[][],
		slots		solardatm.agg_datm[]
	) LANGUAGE SQL STABLE ROWS 1 AS
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

				-- both rows at or after the end of the slot: the difference belongs to a later slot
				-- (two reset records after the end, before the next datum; see NET-540)
				WHEN lag(d.ts) OVER slot >= end_ts
					THEN 0

				ELSE 1
				END as portion
			, (ts < end_ts AND NOT (ts <= start_ts AND rtype = 1)
				OR (ts = end_ts AND rtype < 2)) AS rinc
			-- the time of the row before, for the 15 minute slots
			, lag(d.ts) OVER slot AS p_ts
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

	-- the 15 minute slots of the hour, from here on

	-- slots of the hour holding a datum or reset record
	, occ AS (
		SELECT DISTINCT time_bucket(INTERVAL 'PT15M', d.ts) AS slot
		FROM d
		WHERE d.inc
	)
	-- the first and last of those slots, or the last slot of the hour if there are none
	, edge AS (
		SELECT COALESCE(min(slot), end_ts - INTERVAL 'PT15M') AS first_slot
			 , COALESCE(max(slot), end_ts - INTERVAL 'PT15M') AS last_slot
		FROM occ
	)
	-- instantaneous statistics per slot
	, slot_di_ary AS (
		SELECT
			  time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, vec_trim_scale(vec_agg_mean(d.data_i)) AS data_i
			, solarcommon.array_transpose2d(ARRAY[
				  vec_agg_count(d.data_i)::numeric[]
				, vec_agg_min(d.data_i)
				, vec_agg_max(d.data_i)
			  ]) AS stat_i
		FROM d
		WHERE d.inc
		GROUP BY 1
	)
	-- the slots of each row and of the row before it, when they are within the hour
	, wb AS (
		SELECT wa.*
			, wa.portion::numeric AS hp
			, CASE WHEN wa.ts >= start_ts AND wa.ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.ts) END AS c_slot
			, CASE WHEN wa.p_ts >= start_ts AND wa.p_ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.p_ts) END AS p_slot
		FROM wa
	)
	-- the earlier row's share of the hour's portion, when the two rows are in different slots of the hour;
	-- rows without a value for the property have no difference to share out
	, wc AS (
		SELECT wb.*
			, CASE
				WHEN p_slot IS NOT NULL AND c_slot IS NOT NULL AND c_slot > p_slot
					THEN (EXTRACT('epoch' FROM c_slot - p_ts) / EXTRACT('epoch' FROM ts - p_ts))::numeric
				END AS p_share
		FROM wb
		WHERE hp <> 0 AND val IS NOT NULL
	)
	, alloc AS (
		-- to the slot of the earlier row
		SELECT idx, p_slot AS slot, diff * COALESCE(p_share, hp) AS amt
		FROM wc
		WHERE p_slot IS NOT NULL AND (c_slot IS NULL OR c_slot > p_slot)

		UNION ALL

		-- to the slot of the later row: everything, or the remainder after the earlier row's share
		SELECT idx, c_slot AS slot, diff * (hp - COALESCE(p_share, 0)) AS amt
		FROM wc
		WHERE c_slot IS NOT NULL

		UNION ALL

		-- neither row is within the hour
		SELECT idx, edge.last_slot AS slot, diff * hp AS amt
		FROM wc, edge
		WHERE p_slot IS NULL AND c_slot IS NULL
	)
	-- clock accumulation per slot
	, ca AS (
		SELECT idx, slot, sum(amt) AS cdiff
		FROM alloc
		GROUP BY idx, slot
	)
	-- the rows the hour's readings count, each in its slot
	, rd AS (
		SELECT wa.idx
			, CASE
				WHEN wa.ts < start_ts THEN edge.first_slot
				WHEN wa.ts < end_ts THEN time_bucket(INTERVAL 'PT15M', wa.ts)
				ELSE edge.last_slot
				END AS slot
			, wa.val
			, wa.diff
			, wa.ts
			, wa.rtype
		FROM wa, edge
		WHERE wa.rinc
	)
	-- readings per slot
	, ra0 AS (
		SELECT idx, slot
			, sum(diff) AS rdiff
			, solarcommon.first(val ORDER BY ts, rtype) AS rstart_first
			, solarcommon.first(val - diff ORDER BY ts, rtype) AS rstart_later
			, solarcommon.first(val ORDER BY ts DESC, rtype DESC) AS rend
		FROM rd
		GROUP BY idx, slot
	)
	-- for each property: whether any slot of the hour has a value for it, and the first slot that does
	, ra AS (
		SELECT ra0.*
			, bool_or(ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS hour_has
			, ra0.slot = min(ra0.slot) FILTER (WHERE ra0.rend IS NOT NULL) OVER (PARTITION BY ra0.idx) AS is_first
		FROM ra0
	)
	, slot_ts AS (
		SELECT slot FROM occ
		UNION
		SELECT slot FROM ca
		UNION
		SELECT slot FROM ra
	)
	-- A slot whose rows have no value for a property gets NULL for it when another slot of the hour
	-- has one, so that rolling the slots up takes the hour's first and last readings from slots that
	-- have them. When no slot has a value the hourly rollup reports zero, and so does each slot.
	, slot_da_ary AS (
		SELECT s.slot
			, CASE WHEN count(ca.idx) + count(ra.idx) > 0 THEN
				vec_trim_scale(array_agg(
					CASE
						WHEN ra.rend IS NOT NULL OR ca.cdiff <> 0 THEN COALESCE(ca.cdiff, 0)
						WHEN ra.hour_has THEN NULL
						WHEN ra.idx IS NOT NULL THEN 0
						END
					ORDER BY p.idx))
				END AS data_a
			, CASE WHEN count(ra.idx) > 0 THEN
				array_agg(ARRAY[
					  ra.rdiff
					, CASE WHEN ra.is_first THEN ra.rstart_first ELSE ra.rstart_later END
					, ra.rend
				] ORDER BY p.idx)
				END AS read_a
		FROM slot_ts s
		CROSS JOIN m
		CROSS JOIN generate_series(1, m.len_a) AS p(idx)
		LEFT OUTER JOIN ca ON ca.slot = s.slot AND ca.idx = p.idx
		LEFT OUTER JOIN ra ON ra.slot = s.slot AND ra.idx = p.idx
		GROUP BY s.slot
	)
	-- status per slot, as most-frequent status values
	, slot_ws AS (
		SELECT
			  time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, p.idx AS idx
			, d.data_s[p.idx] AS val
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_s, 1)) AS p(idx) ON TRUE
		WHERE d.data_s IS NOT NULL AND d.inc
	)
	, slot_ds AS (
		SELECT slot_ws.slot, slot_ws.idx, mode() WITHIN GROUP (ORDER BY slot_ws.val) AS val
		FROM slot_ws
		GROUP BY slot_ws.slot, slot_ws.idx
	)
	, slot_ds_ary AS (
		SELECT slot, array_agg(val ORDER BY idx) AS data_s
		FROM slot_ds
		GROUP BY slot
	)
	-- tags per slot
	, slot_dt_ary AS (
		SELECT time_bucket(INTERVAL 'PT15M', d.ts) AS slot
			, array_agg(DISTINCT p.val) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL AND d.inc
		GROUP BY 1
	)
	-- the slot rows, in time order
	, slot_ary AS (
		SELECT array_agg(ROW(
				  sid
				, slot_ts.slot
				, slot_di_ary.data_i
				, slot_da_ary.data_a
				, slot_ds_ary.data_s
				, slot_dt_ary.data_t
				, slot_di_ary.stat_i
				, slot_da_ary.read_a
			)::solardatm.agg_datm ORDER BY slot_ts.slot) AS slots
		FROM slot_ts
		LEFT OUTER JOIN slot_di_ary ON slot_di_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_da_ary ON slot_da_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_ds_ary ON slot_ds_ary.slot = slot_ts.slot
		LEFT OUTER JOIN slot_dt_ary ON slot_dt_ary.slot = slot_ts.slot
		WHERE slot_di_ary.data_i IS NOT NULL OR slot_da_ary.data_a IS NOT NULL
			OR slot_ds_ary.data_s IS NOT NULL OR slot_dt_ary.data_t IS NOT NULL
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
		, slot_ary.slots
	FROM di_ary, da_ary, ds_ary, dt_ary, slot_ary
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
 * The start and end readings of each accumulating property in `read_a` are those of the first and
 * last row that has them, preferring rows whose accumulation in `data_a` is not NULL.
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
			, d.ts_start
			, d.data_a[p.idx] AS val
			, d.read_a[p.idx][1] AS rdiff
			, d.read_a[p.idx][2] AS rstart
			, d.read_a[p.idx][3] AS rend
		FROM d
		INNER JOIN generate_series(1, array_upper(d.data_a, 1)) AS p(idx) ON TRUE
	)
	-- calculate accumulating statistics; the start and end readings are those of the first and
	-- last row that has them, taking rows whose accumulation is not NULL first (see NET-544)
	, da AS (
		SELECT
			idx
			, sum(val) AS val
			, sum(rdiff) AS rdiff
			, (array_agg(rstart ORDER BY val IS NULL, ts_start) FILTER (WHERE rstart IS NOT NULL))[1] AS rstart
			, (array_agg(rend ORDER BY val IS NULL, ts_start DESC) FILTER (WHERE rend IS NOT NULL))[1] AS rend
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
