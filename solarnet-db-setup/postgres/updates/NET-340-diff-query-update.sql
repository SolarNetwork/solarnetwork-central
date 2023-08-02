/**
 * Find the datum that exist immediately before and after a point in time for a stream, within a
 * time tolerance.
 *
 * If a datum exists exactly at the given timestamp, that datum alone will be returned.
 * Otherwise up to two datum will be returned, one immediately before and one immediately after
 * the given timestamp.
 *
 * @param sid 				the stream ID of the datum that has been changed (inserted, deleted)
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param tolerance 		the maximum time to look forward/backward for adjacent datm
 * @param must_a			if TRUE then only consider rows where data_a is not NULL
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_around(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '1 months',
		must_a		BOOLEAN DEFAULT FALSE
	) RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 2 AS
$$
	WITH b AS (
		-- exact
		(
			SELECT d.*, 0 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts = ts_at
				AND (NOT must_a OR d.data_a IS NOT NULL)
		)
		UNION ALL
		-- prev
		(
			SELECT d.*, 1 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts < ts_at
				AND d.ts > ts_at - tolerance
				AND (NOT must_a OR d.data_a IS NOT NULL)
			ORDER BY d.stream_id, d.ts DESC
			LIMIT 1
		)
		UNION ALL
		-- next
		(
			SELECT d.*, 1 AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts > ts_at
				AND d.ts < ts_at + tolerance
				AND (NOT must_a OR d.data_a IS NOT NULL)
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
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


CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_near_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance 		INTERVAL DEFAULT INTERVAL '3 months'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	-- choose earliest first/last rows for start/end dates, which may be exact time matches
	WITH d AS (
		(
		SELECT d.*, 0::SMALLINT AS rtype
		FROM solardatm.find_datm_around(sid, start_ts, tolerance, TRUE) AS d
		ORDER BY d.ts
		LIMIT 1
		)
		UNION
		(
		SELECT d.*, 0::SMALLINT AS rtype
		FROM solardatm.find_datm_around(sid, end_ts, tolerance, TRUE) AS d
		ORDER BY d.ts
		LIMIT 1
		)
	)
	, drange AS (
		SELECT
			  COALESCE(min(ts), start_ts) AS ts_min
			, COALESCE(max(ts), end_ts) AS ts_max
		FROM d
	)
	, resets AS (
		SELECT
			  aux.stream_id
			, aux.ts
			, aux.data_a
			, aux.rtype AS rtype
		FROM drange, solardatm.find_datm_aux_for_time_span(
			sid,
			LEAST(drange.ts_min, start_ts),
			GREATEST(drange.ts_max, end_ts)
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
				SELECT COALESCE(max(ts), end_ts) AS max_ts
				FROM (
					SELECT max(ts) FROM d WHERE ts <= end_ts
					UNION ALL
					SELECT max(ts) FROM resets WHERE ts <= end_ts
				) r(ts)
			) max
	)
	-- combine raw datm with reset datm
	SELECT d.stream_id
		, d.ts
		, NULL::NUMERIC[] AS data_i
		, d.data_a
		, NULL::TEXT[] AS data_s
		, NULL::TEXT[] AS data_t
		, d.rtype
	FROM d, ts_range
	WHERE d.ts >= ts_range.min_ts AND d.ts <= ts_range.max_ts
	UNION ALL
	SELECT resets.stream_id
		, resets.ts
		, NULL::NUMERIC[] AS data_i
		, resets.data_a
		, NULL::TEXT[] AS data_s
		, NULL::TEXT[] AS data_t
		, resets.rtype
	FROM resets, ts_range
	WHERE resets.ts >= ts_range.min_ts
		-- exclude any reading start record at exactly the end date
		AND (resets.ts < end_ts OR resets.rtype < 2)
$$;



DROP FUNCTION IF EXISTS solardatm.find_datm_around(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL
	);
