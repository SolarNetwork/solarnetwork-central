/**
 * Find a datum time immediately earlier in time to a given instant, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_time_before(
	sid 		UUID,
	ts_at 		TIMESTAMP WITH TIME ZONE,
	cutoff 		TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STRICT STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts < ts_at
		AND ts >= cutoff
	ORDER BY ts DESC
	LIMIT 1
$$;

/**
 * Find a datum time immediately earlier in time to a given instant that MUST have an accumulating
 * property value, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look backward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_time_before_with_a(
	sid 		UUID,
	ts_at 		TIMESTAMP WITH TIME ZONE,
	cutoff 		TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STRICT STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts < ts_at
		AND ts >= cutoff
		AND data_a IS NOT NULL
	ORDER BY ts DESC
	LIMIT 1
$$;

/**
 * Find a datum time immediately after in time to a given instant, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look forward for adjacent datm
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_time_after(
	sid UUID,
	ts_at TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STRICT STABLE ROWS 1 AS
$$
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts > ts_at
		AND ts <= cutoff
	ORDER BY ts
	LIMIT 1
$$;

CREATE OR REPLACE FUNCTION solardatm.find_datm_for_time_slot2(
		sid 		UUID,
		start_ts 	TIMESTAMP WITH TIME ZONE,
		end_ts 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT INTERVAL 'P3M',
		target_agg 	INTERVAL DEFAULT INTERVAL 'PT1H'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE plpgsql STRICT STABLE ROWS 200 AS
$$
DECLARE
	stream_has_no_a 	BOOLEAN;
	stream_min_ts		TIMESTAMP WITH TIME ZONE := start_ts - tolerance;
	drange_min_ts		TIMESTAMP WITH TIME ZONE;
	drange_min_has_a 	BOOLEAN;
	drange_max_ts		TIMESTAMP WITH TIME ZONE;
	srange_min_ts		TIMESTAMP WITH TIME ZONE;
	srange_min_has_a 	BOOLEAN;
	srange_max_ts		TIMESTAMP WITH TIME ZONE;
	tolerance_a			INTERVAL := LEAST(tolerance, INTERVAL 'P2D');
BEGIN
	-- find if stream even has accumulating properties, to avoid costly scan
	SELECT COALESCE(CARDINALITY(names_a) = 0, TRUE)
	FROM solardatm.find_metadata_for_stream(sid)
	INTO stream_has_no_a;

	-- Find min/max datum dates WITHIN slot.
	--
	-- Note that an INCLUSIVE end date it used to pick up a "gap" slot that could occur leading up
	-- to the end time slot (see NET-469).


	-- find minimum datum date within slot, tracking also if it has an accumulating value
	SELECT ts, COALESCE(CARDINALITY(data_a) > 0, FALSE)
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts <= end_ts
	ORDER BY ts
	LIMIT 1
	INTO drange_min_ts, drange_min_has_a;

	-- find maximum datum date within slot
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts <= end_ts
	ORDER BY ts DESC
	LIMIT 1
	INTO drange_max_ts;

	-- find prior datum date before drange_min_ts for reading input
	--
	-- if drange_min_ts == start_ts AND drange_min_has_a we know our minimum already
	IF drange_min_ts = start_ts AND (stream_has_no_a OR COALESCE(drange_min_has_a, FALSE)) THEN
		-- previously found min datum at exact start AND has required accumulating value
		srange_min_ts := start_ts;
	ELSIF stream_has_no_a THEN
		-- no accumulating properties, do simple search for previous datum
		SELECT ts
		FROM solardatm.find_datum_ts_before(sid, drange_min_ts, stream_min_ts) AS d(ts)
		INTO srange_min_ts;
	ELSE
		-- find prior datum date before minimum within slot REQUIRING accumulating value
		-- but use forced shorter tolerance because REQUIRING accumulating too expensive
		SELECT ts
		FROM solardatm.find_datm_time_before_with_a(
				  sid
				, COALESCE(drange_min_ts, start_ts)
				, start_ts - tolerance_a
			) AS d(ts)
		INTO srange_min_ts;

		IF NOT FOUND THEN
			-- did not find datum with accumulating value within SHORTER tolerance, so switch
			-- to fast search for any datum within FULL tolerance
			SELECT d.ts, d.has_a
			FROM solardatm.find_datum_ts_before(sid, start_ts - tolerance_a, stream_min_ts) AS dt(ts)
				, solardatm.datm_has_accumulating_at(sid, dt.ts) AS d
			INTO srange_min_ts, srange_min_has_a;

			IF FOUND THEN
				IF NOT srange_min_has_a THEN
					-- try another SHORT slow search near found time for accumulating properties
					SELECT ts
					FROM solardatm.find_datm_time_before_with_a(
							  sid
							, srange_min_ts
							, GREATEST(srange_min_ts - tolerance_a, stream_min_ts)
						) AS d(ts)
					INTO srange_min_ts;
				END IF;
			END IF;
		END IF;
	END IF;

	IF drange_max_ts = end_ts THEN
		-- previously found max datum at exact end
		srange_max_ts = end_ts;
	ELSE
		-- find next datum date after maximum within slot (for instantaneous calc)
		SELECT ts
		FROM solardatm.find_datm_time_after(sid, drange_max_ts, end_ts + tolerance) AS d(ts)
		INTO srange_max_ts;
	END IF;

	RETURN QUERY

	-- find date range for resets
	WITH reset_range AS (
		SELECT MIN(aux.ts) AS min_ts, MAX(aux.ts) AS max_ts
		FROM solardatm.da_datm_aux aux
		WHERE aux.atype = 'Reset'::solardatm.da_datm_aux_type
			AND aux.stream_id = sid
			AND aux.ts >= CASE
					WHEN srange_min_ts <= start_ts THEN srange_min_ts
					ELSE start_ts - tolerance
				END
			AND aux.ts <= CASE
				WHEN srange_max_ts >= end_ts THEN srange_max_ts
				ELSE end_ts + tolerance
			END
	)

	-- get combined range for datum + resets
	, combined_srange AS (
		SELECT CASE
				-- if datum falls exactly on start, only include prior datum if it is more than
				-- one agg slot away; otherwise that datum will be included in prior slot
				WHEN t.min_ts IS NULL OR (
						drange_min_ts = start_ts
						AND drange_min_has_a
						AND (start_ts - t.min_ts) < target_agg
					)
					THEN drange_min_ts
				ELSE t.min_ts
			END AS min_ts
			, CASE
				WHEN t.max_ts IS NULL OR drange_max_ts = end_ts THEN drange_max_ts
				ELSE t.max_ts
			END AS max_ts
		FROM (
			SELECT CASE
					-- start < reset < datum: reset is min
					WHEN reset_range.min_ts < srange_min_ts
						AND reset_range.min_ts >= start_ts
						THEN reset_range.min_ts

					-- datum < reset < start: reset is min
					WHEN reset_range.min_ts > srange_min_ts
						AND reset_range.min_ts <= start_ts
						THEN reset_range.min_ts

					-- no datum but reset: reset is min
					WHEN drange_min_ts IS NULL
						AND reset_range.min_ts IS NOT NULL
						THEN LEAST(srange_min_ts, reset_range.min_ts)

					-- no datum
					WHEN drange_min_ts IS NULL THEN start_ts

					-- otherwise: datum is min (or null)
					ELSE srange_min_ts
				END AS min_ts
				, CASE
					-- datum < reset < end: reset is max
					WHEN reset_range.max_ts > srange_max_ts
						AND reset_range.max_ts <= end_ts
						THEN reset_range.max_ts

					-- end < reset < datum: reset is max (or null)
					WHEN reset_range.max_ts < srange_max_ts
						AND reset_range.max_ts >= end_ts
						THEN reset_range.max_ts

					-- no datum but reset: reset is max (or null)
					WHEN drange_max_ts IS NULL
						AND reset_range.max_ts IS NOT NULL
						THEN LEAST(srange_max_ts, reset_range.max_ts)

					-- no datum
					WHEN drange_max_ts IS NULL THEN end_ts

					-- otherwise: datum is max (or null)
					ELSE srange_max_ts
				END AS max_ts
			FROM reset_range
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
	) aux;
END
$$;
