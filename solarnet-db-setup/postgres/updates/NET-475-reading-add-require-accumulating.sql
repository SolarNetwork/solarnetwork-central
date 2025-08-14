/**
 * Find a datum time immediately earlier in time a given instance, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look backward for adjacent datm
 * @param must_a			if TRUE then only consider rows where data_a is not NULL
 * @param has_no_a			TRUE if the stream can be assumed NOT to have accumulating properties
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_before_ts(
	sid 		UUID,
	ts_at 		TIMESTAMP WITH TIME ZONE,
	cutoff 		TIMESTAMP WITH TIME ZONE,
	must_a		BOOLEAN DEFAULT FALSE,
	has_no_a 	BOOLEAN DEFAULT FALSE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts < ts_at
		AND ts >= cutoff
		AND NOT(must_a AND (has_no_a OR data_a IS NULL))
	ORDER BY ts DESC
	LIMIT 1
$$;

-- update to use solardatm.find_time_before_ts
DROP FUNCTION solardatm.find_time_before(
	UUID,
	TIMESTAMP WITH TIME ZONE,
	TIMESTAMP WITH TIME ZONE);
CREATE OR REPLACE FUNCTION solardatm.find_time_before(
	sid UUID,
	ts_at TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT * FROM solardatm.find_time_before_ts(sid, ts_at, cutoff, FALSE, FALSE);
$$;



/**
 * Find a datum time immediately later in time a given instance, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look forward for adjacent datm
 * @param must_a			if TRUE then only consider rows where data_a is not NULL
 * @param has_no_a			TRUE if the stream can be assumed NOT to have accumulating properties
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_after_ts(
	sid 		UUID,
	ts_at 		TIMESTAMP WITH TIME ZONE,
	cutoff 		TIMESTAMP WITH TIME ZONE,
	must_a		BOOLEAN DEFAULT FALSE,
	has_no_a 	BOOLEAN DEFAULT FALSE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts > ts_at
		AND ts <= cutoff
		AND NOT(must_a AND (has_no_a OR data_a IS NULL))
	ORDER BY ts
	LIMIT 1
$$;

DROP FUNCTION solardatm.find_time_after(
	UUID,
	TIMESTAMP WITH TIME ZONE,
	TIMESTAMP WITH TIME ZONE);
CREATE OR REPLACE FUNCTION solardatm.find_time_after(
	sid UUID,
	ts_at TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT * FROM solardatm.find_time_after_ts(sid, ts_at, cutoff, FALSE, FALSE);
$$;


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
					FROM meta, drange, solardatm.find_time_before_ts(sid, drange.min_ts, start_ts - LEAST(tolerance, INTERVAL 'P14D'), TRUE, meta.has_no_a) AS d(ts)
				),
				(
					-- find prior datum date before minimum within slot (or exact start of slot) NOT REQUIRING accumulating
					-- using full tolerance because index-only scan possible and thus fast enough
					SELECT CASE
						WHEN d.ts IS NULL THEN drange.min_ts
						WHEN drange.min_ts = start_ts AND drange.min_has_a THEN drange.min_ts
						ELSE d.ts
					END
					FROM meta, drange, solardatm.find_time_before_ts(sid, drange.min_ts, start_ts - tolerance, FALSE, meta.has_no_a) AS d(ts)
				)
			) AS min_ts
			, (
				-- find next datum date after maximum within slot (or exact end of slot)
				SELECT CASE
					WHEN d.ts IS NULL THEN drange.max_ts
					WHEN drange.max_ts = end_ts THEN drange.max_ts
					ELSE d.ts
				END
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

-- align with logic above
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
				-- find prior datum date before ts NOT REQUIRING accumulating
				-- using full tolerance because index-only scan possible and thus fast enough
				-- but then join back to solardatm.da_datm to restrict on must_a
				WITH t AS (
					SELECT d.ts
					FROM meta, solardatm.find_time_before_ts(sid, ts_at, ts_at - tolerance, FALSE, meta.has_no_a) AS d(ts)
				)
				SELECT t.ts
				FROM t
				INNER JOIN solardatm.da_datm d ON d.stream_id = sid AND d.ts = t.ts
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
				-- but then join back to solardatm.da_datm to restrict on must_a
				WITH t AS (
					SELECT d.ts
					FROM meta, solardatm.find_time_after_ts(sid, ts_at, ts_at + tolerance, FALSE, meta.has_no_a) AS d(ts)
				)
				SELECT t.ts
				FROM t
				INNER JOIN solardatm.da_datm d ON d.stream_id = sid AND d.ts = t.ts
				WHERE (NOT must_a OR d.data_a IS NOT NULL)
			)
		) AS ts
	)
	, b AS (
		SELECT d.*, 0 AS rtype
		FROM exact d

		UNION ALL

		SELECT d.*, 1 AS rtype
		FROM srange
		INNER JOIN solardatm.da_datm d ON d.stream_id = sid AND d.ts = srange.ts
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
$$;
