CREATE OR REPLACE FUNCTION solardatm.find_datm_hours(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		stream_id 		UUID,
		ts_start		TIMESTAMP WITH TIME ZONE
	) LANGUAGE SQL STABLE ROWS 2000 AS
$$
	-- Include previous adjacent hour if first datum in hour falls exactly on hour,
	-- to account for possible accumulation after an hour gap (NET-469). The outer
	-- query then strips out any matching end date, making it exclusive again.
	SELECT sid, ts_start
	FROM (
		SELECT DISTINCT UNNEST(CASE
				WHEN date_trunc('hour', ts) = min(ts) THEN ARRAY[date_trunc('hour', ts) - INTERVAL 'PT1H', date_trunc('hour', ts)]
				ELSE ARRAY[date_trunc('hour', ts)]
			END) AS ts_start
		FROM (
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts

			UNION ALL

			SELECT ts
			FROM solardatm.da_datm_aux
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
				AND atype = 'Reset'::solardatm.da_datm_aux_type
		)
		GROUP BY date_trunc('hour', ts)
	)
	WHERE ts_start < end_ts
$$;

CREATE OR REPLACE FUNCTION solardatm.find_datm_for_time_slot(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT INTERVAL 'P3M',
		target_agg 	INTERVAL DEFAULT INTERVAL 'PT1H'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 200 AS
$$
	-- Find min/max datum date within slot; if no actual data in this slot we coalesce to the
	-- start_ts - end_ts input range. Note that an INCLUSIVE end date is used to pick up a "gap"
	-- slot that could occur leading up to the end time slot (see NET-469).
	WITH drange AS (
		SELECT COALESCE((
			-- find minimum datum date within slot
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY stream_id, ts
			LIMIT 1
		), start_ts) AS min_ts
		, COALESCE((
			-- find maximum datum date within slot
			SELECT ts
			FROM solardatm.da_datm
			WHERE stream_id = sid
				AND ts >= start_ts
				AND ts <= end_ts
			ORDER BY stream_id, ts DESC
			LIMIT 1
		), end_ts) AS max_ts
	)

	-- find prior/next datum date range to provide for clock and reading input
	, srange AS (
		SELECT COALESCE(t.min_ts, drange.min_ts) AS min_ts, COALESCE(t.max_ts, drange.max_ts) AS max_ts
		FROM drange, (
			SELECT (
				-- find prior datum date before minimum within slot (or exact start of slot)
				SELECT CASE
					WHEN d.ts IS NULL THEN drange.min_ts
					WHEN drange.min_ts = start_ts THEN drange.min_ts
					ELSE d.ts
				END
				FROM drange, solardatm.find_time_before(sid, drange.min_ts, start_ts - tolerance) AS d(ts)
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
