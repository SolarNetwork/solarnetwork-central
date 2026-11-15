/* ============================================================================
 * NET-538: find the datum before a time past a "way-back" datum that has no accumulating
 * properties, and let PostgreSQL inline the two functions that do this.
 *
 * `solardatm.find_datm_for_time_slot()` and `solardatm.find_datm_around_ts()` first look for the
 * datum before a time within the 14 days before it. If none is found there, they take the latest
 * datum further back within the tolerance (the "way-back" datum). When that datum has no
 * accumulating properties, they were meant to search the 14 days before it for one that has.
 * That search never ran: the `way_back_d` CTE kept the way-back datum only when it had
 * accumulating properties, so the search, which starts from `way_back_d`, had no row to start
 * from. No datum before the time was used at all, and the reading difference across more than
 * 14 days without accumulating properties was lost from the hour that follows them.
 *
 * In `find_datm_around_ts()` with `must_a` FALSE the same filter also dropped a way-back datum
 * that had no accumulating properties, although none were required. So `find_datm_around()` and
 * `calc_datm_at()` found no datum before a time that was more than 14 days after the previous
 * datum, unless that datum had accumulating properties.
 *
 * The search before the way-back datum now runs. It stays within the tolerance, and is skipped
 * when the stream has no accumulating properties.
 *
 * Neither function is declared STRICT any more. PostgreSQL never inlines a STRICT set-returning
 * SQL function, so each call planned the function's query on its own, without its argument
 * values, and so over every `da_datm` chunk. Inlined, the query is planned as part of the calling
 * query, with the argument values that query has, and only the chunks that can match are used.
 * A NULL argument still gives no rows.
 *
 * `find_datm_around_ts()` also now looks up the datum at the times it finds with
 * `solardatm.find_datm_between()`, instead of joining `da_datm` on those times. A time found
 * while the query runs cannot exclude any chunks when the query is planned, so once the function
 * was inlined each of its three such joins was planned over every chunk. `find_datm_between()`
 * plans its own query with the time as a value.
 *
 * The function signatures are unchanged, so `CREATE OR REPLACE` keeps their existing owner and
 * ACLs.
 * ============================================================================
 */

/**
 * Find the datum that exist immediately before and after a point in time for a stream, within a
 * time tolerance.
 *
 * If a datum exists exactly at the given timestamp, that datum alone will be returned.
 * Otherwise up to two datum will be returned, one immediately before and one immediately after
 * the given timestamp.
 *
 * The has_no_a argument can be calculated like:
 *
 * ```
 * SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE)
 * FROM solardatm.find_metadata_for_stream(sid)
 * ```
 *
 * The datum before `ts_at` is first searched for within the 14 days before it (or the tolerance,
 * if shorter). If none is found there, the latest datum before those 14 days and within the
 * tolerance is used, unless `must_a` is TRUE and it has no accumulating properties: then the
 * latest datum with accumulating properties within the 14 days before it, and within the
 * tolerance, is used instead.
 *
 * This function is not declared STRICT, so that PostgreSQL can inline it into the calling query
 * and plan it with the argument values given, using only the chunks that can match. It returns
 * no rows if any argument is NULL, as a STRICT function would. For the same reason it looks up
 * the datum at the times it finds with `solardatm.find_datm_between()`, which plans its query
 * with the time as a value: a join on a time found while the query runs would be planned over
 * every chunk.
 *
 * @param sid 				the stream ID of the datum that has been changed (inserted, deleted)
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 * @param must_a			if TRUE then only consider rows where data_a is not NULL
 * @param has_no_a			TRUE if the stream can be assumed NOT to have accumulating properties
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_around_ts(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval 'P1M',
		must_a		BOOLEAN DEFAULT FALSE,
		has_no_a 	BOOLEAN DEFAULT FALSE
	) RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 2 AS
$$
	-- find if stream even has accumulating properties, to avoid costly scan
	WITH meta AS (
		SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE) AS has_no_a
 		FROM solardatm.find_metadata_for_stream(sid)
	)
	, exact AS (
		SELECT d.*
		FROM solardatm.da_datm d
		WHERE d.stream_id = sid
			AND d.ts = ts_at
			AND NOT(must_a AND (has_no_a OR d.data_a IS NULL))
	)
	, srange AS (
		SELECT COALESCE(
			(
				-- find prior datum date before ts REQUIRING accumulating
				-- but use forced shorter tolerance because REQUIRING accumulating too expensive
				SELECT d.ts
				FROM meta, solardatm.find_time_before_ts(sid, ts_at, ts_at - LEAST(tolerance, INTERVAL 'P14D'), must_a, meta.has_no_a) AS d(ts)
			),
			(
				-- expand search past 14d slow search:
				-- find prior datum date before ts NOT REQUIRING accumulating
				-- using full tolerance because index-only scan possible and thus fast enough
				-- but then look up the datum to restrict on must_a
				WITH way_back AS (
					SELECT d.ts
					FROM meta, solardatm.find_time_before_ts(
						  sid
						, ts_at - LEAST(tolerance, INTERVAL 'P14D')
						, ts_at - tolerance
						, FALSE
						, meta.has_no_a
						) AS d(ts)
				)
				-- find datum with way_back time, to see if has accumulating properties
				, way_back_d AS (
					SELECT d.ts, d.has_a
					FROM way_back, solardatm.datm_has_accumulating_at(sid, way_back.ts) AS d
				)
				, t AS (
					SELECT COALESCE(
						(
							-- if way-back datum has accumulating properties, or they are not
							-- required, use it
							SELECT ts
							FROM way_back_d
							WHERE has_a OR NOT must_a
						),
						(
							-- otherwise try short slow search before way-back datum for accumulating
							-- properties, within the tolerance, unless stream has none (see NET-538)
							SELECT d.ts
							FROM meta, way_back_d, solardatm.find_time_before_ts(
								  sid
								, way_back_d.ts
								, GREATEST(way_back_d.ts - LEAST(tolerance, INTERVAL 'P14D'), ts_at - tolerance)
								, TRUE
								, meta.has_no_a
							) AS d(ts)
							WHERE NOT meta.has_no_a
						)
					) AS ts
				)
				-- a lookup rather than a join, so it is not planned over every chunk (see NET-538)
				SELECT d.ts
				FROM t, solardatm.find_datm_between(sid, t.ts, t.ts) d
				WHERE (NOT must_a OR d.data_a IS NOT NULL)
			)
		) AS ts

		UNION ALL

		SELECT COALESCE(
			(
				-- find next datum date after ts REQUIRING accumulating
				-- but use forced shorter tolerance because REQUIRING accumulating too expensive
				SELECT d.ts
				FROM meta, solardatm.find_time_after_ts(sid, ts_at, ts_at + LEAST(tolerance, INTERVAL 'P14D'), must_a, meta.has_no_a) AS d(ts)
			),
			(
				-- find next datum date after ts NOT REQUIRING accumulating
				-- using full tolerance because index-only scan possible and thus fast enough
				-- but then look up the datum to restrict on must_a
				WITH t AS (
					SELECT d.ts
					FROM meta, solardatm.find_time_after_ts(sid, ts_at, ts_at + tolerance, FALSE, meta.has_no_a) AS d(ts)
				)
				-- a lookup rather than a join, so it is not planned over every chunk (see NET-538)
				SELECT d.ts
				FROM t, solardatm.find_datm_between(sid, t.ts, t.ts) d
				WHERE (NOT must_a OR d.data_a IS NOT NULL)
			)
		) AS ts
	)
	, b AS (
		SELECT d.*, 0 AS rtype
		FROM exact d

		UNION ALL

		-- a lookup rather than a join, so it is not planned over every chunk (see NET-538)
		SELECT d.*, 1 AS rtype
		FROM srange, solardatm.find_datm_between(sid, srange.ts, srange.ts) d
	)
	, d AS (
		-- choose exact if available, fall back to before/after otherwise
		SELECT b.*
			, CASE
				WHEN rtype = 0 THEN TRUE
				WHEN rtype = 1 AND rank() OVER (ORDER BY rtype) = 1 THEN TRUE
				ELSE FALSE
				END AS inc
		FROM b
	)
	SELECT stream_id, ts, received, data_i, data_a, data_s, data_t
	FROM d
	WHERE inc
		-- a NULL argument gives no rows, as for a STRICT function (see NET-538)
		AND num_nulls(sid, ts_at, tolerance, must_a, has_no_a) = 0
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

	-- find prior/next datum date range to provide for clock and reading input
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts, start_ts) AS min_ts, COALESCE(t.max_ts, drange.max_ts, end_ts) AS max_ts
		FROM drange, (
			SELECT COALESCE(
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
			) AS min_ts
			, (
				-- find next datum date after maximum within slot (or exact end of slot) REQUIRING accumulating;
				-- the next datum only contributes to the clock accumulation that crosses the end of the slot,
				-- which is ignored beyond the 1 hour clock tolerance, so limiting the search to that keeps
				-- REQUIRING accumulating fast even when no such datum exists (see NET-537)
				SELECT CASE
					WHEN d.ts IS NULL THEN drange.max_ts
					WHEN drange.max_ts = end_ts THEN drange.max_ts
					ELSE d.ts
				END
				FROM meta, drange, solardatm.find_time_after_ts(sid, drange.max_ts, end_ts + LEAST(tolerance, INTERVAL 'PT1H'), TRUE, meta.has_no_a) AS d(ts)
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
