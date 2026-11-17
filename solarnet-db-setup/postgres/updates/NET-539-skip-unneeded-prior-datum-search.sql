/* ============================================================================
 * NET-539: search for the datum before an aggregate slot only when something in the slot, or
 * the next datum after it, can be differenced against it.
 *
 * `solardatm.find_datm_for_time_slot()` finds the datum with accumulating properties before a
 * slot, for the reading difference and the clock accumulation that cross the start of the slot.
 * It searches the 14 days before the slot and, if that finds nothing, the 14 days before the
 * latest datum further back (see NET-538). Each search reads every row from the start of its
 * window back to the first one with accumulating properties, so every row in the window when it
 * has none. So each hourly rollup of a stream that declares accumulating properties but has posted
 * none for 14 days read up to 28 days of rows and found nothing: about 40,800 buffers for a stream
 * posting once a minute, where an ordinary stream needs about 190.
 *
 * The search now runs only when the prior datum is needed, which is when at least one of these
 * holds:
 *
 *  - a datum in the slot has accumulating properties, counting one exactly at the end of the slot,
 *    which is returned for the reading that ends the slot (see NET-469);
 *  - a reset record is in the slot, counting one exactly at its start or end: its final reading is
 *    differenced against the prior datum, and the prior datum also limits the search for reset
 *    records, so that an older reset record does not hide one at the start of the slot;
 *  - the next datum with accumulating properties after the slot is returned (see NET-537): the
 *    clock accumulation from the prior datum to it crosses the slot, and
 *    `solardatm.rollup_datm_for_time_span_slots()` gives part of it to the sub-hour slot that
 *    holds the next datum.
 *
 * Otherwise nothing returned can be differenced against the prior datum, and not searching gives
 * the same result as a search that finds nothing: only the datum in the slot are returned.
 * Deciding this reads only rows in the slot, which the rollup reads anyway, and for most streams
 * the first datum in the slot decides it.
 *
 * Hourly aggregates computed from now on change for an hour that has no datum with accumulating
 * properties, no reset record, and no datum with accumulating properties within the hour after
 * it, when the search would have found a datum with accumulating properties before it:
 *
 *  - an hour with datum now gets NULL `data_a` and `read_a`, where it got a `data_a` of `{0}` and a
 *    `read_a` carrying the prior datum's reading forward with no difference (`{{0,v,v}}`); NULL is
 *    what an hour more than 14 days after the last such datum, or an hour of a stream that never
 *    posts accumulating properties, already got;
 *  - the same holds when a reset record lies between the prior datum and the hour, whose start
 *    reading was carried forward instead (`{{0,s,s}}`);
 *  - an hour with no datum at all after such a reset record now gets no aggregate, where it got a
 *    `data_a` of `{0}` and a `read_a` of `{{0,s,s}}`.
 *
 * Existing aggregates are not recomputed: they keep their values until something marks them
 * stale.
 *
 * `solardatm.rollup_datm_for_time_span_slots()` changes only when its range starts part way
 * through a sub-hour slot, the datum in the range have no accumulating properties, and the prior
 * datum falls in that first slot: the prior datum's instantaneous values are no longer averaged
 * into that slot, as they already were not for a stream without accumulating properties.
 *
 * The function is still a single SQL `SELECT` and not STRICT, so PostgreSQL still inlines it (see
 * NET-538). Its new lookups use `solardatm.da_datm` and `solardatm.da_datm_aux` with the
 * function's arguments only. The function signature is unchanged, so `CREATE OR REPLACE` keeps its
 * existing owner and ACLs.
 * ============================================================================
 */

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
