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
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STRICT STABLE ROWS 200 AS
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
						WHERE d.has_a = TRUE
					)
					SELECT COALESCE(
						(
							-- if way-back datum has accumulating properties, use it
							SELECT ts
							FROM way_back_d
							WHERE has_a
						),
						(
							-- try short slow search near found time for accumulating properties
							SELECT d.ts
							FROM meta, way_back_d, solardatm.find_time_before_ts(
								  sid
								, way_back_d.ts
								, way_back_d.ts - LEAST(tolerance, INTERVAL 'P14D')
								, TRUE
								, meta.has_no_a
							) AS d(ts)
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
