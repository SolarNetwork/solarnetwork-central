/**
 * Find stream datum records for calculating the difference between two dates within a time
 * tolerance.
 *
 * @param sid 				the stream id
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 * @param tolerance 		the maximum time to look forward/backward for adjacent data
 */
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
		FROM solardatm.find_datm_around(sid, start_ts, tolerance) AS d
		ORDER BY d.ts
		LIMIT 1
		)
		UNION
		(
		SELECT d.*, 0::SMALLINT AS rtype
		FROM solardatm.find_datm_around(sid, end_ts, tolerance) AS d
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


/**
 * Find stream datum records for calculating the difference between two dates.
 *
 * @param sid 				the stream id
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	-- we just set a large tolerance for this query
	SELECT * FROM solardatm.find_datm_diff_near_rows(sid, start_ts, end_ts, INTERVAL 'P1Y')
$$;


/**
 * Find stream datum records for calculating the difference within two dates.
 *
 * @param sid 				the stream id
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_within_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	WITH d AS (
		-- next after start
		(
			SELECT d.*, 0::SMALLINT AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts >= start_ts
				AND d.ts < end_ts
				AND d.data_a IS NOT NULL
			ORDER BY d.stream_id, d.ts
			LIMIT 1
		)
		UNION
		-- prev before end
		(
			SELECT d.*, 0::SMALLINT AS rtype
			FROM solardatm.da_datm d
			WHERE d.stream_id = sid
				AND d.ts > start_ts
				AND d.ts <= end_ts
				AND d.data_a IS NOT NULL
			ORDER BY d.stream_id, d.ts DESC
			LIMIT 1
		)
	)
	, resets AS (
		SELECT
			  aux.stream_id
			, aux.ts
			, aux.data_a
			, aux.rtype AS rtype
		FROM solardatm.find_datm_aux_for_time_span(
			sid,
			start_ts,
			end_ts
		) aux
	)
	-- find min, max ts out of raw + resets to eliminate extra leading/trailing from combined results
	, ts_range AS (
		SELECT min_ts, max_ts
		FROM (
				SELECT COALESCE(max(ts), start_ts) AS min_ts
				FROM (
					SELECT max(ts) FROM d WHERE ts < start_ts
					UNION ALL
					SELECT max(ts) FROM resets WHERE ts <= start_ts
				) l(ts)
			) min, (
				SELECT COALESCE(max(ts), end_ts) AS max_ts
				FROM (
					SELECT max(ts) FROM d WHERE ts < end_ts
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


/**
 * Find stream datum records for calculating the difference between two exact dates.
 *
 * @param sid 				the stream id
 * @param start_ts			the minimum date (exact)
 * @param end_ts 			the maximum date (exact)
 * @param tolerance 		the maximum time to look forward/backward for adjacent data
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_diff_at_rows(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE,
		tolerance 		INTERVAL DEFAULT INTERVAL '3 months'
	) RETURNS SETOF solardatm.datm_rec LANGUAGE SQL STABLE ROWS 10 AS
$$
	WITH d AS (
		SELECT (solardatm.calc_datm_at(d, start_ts)).*
		FROM solardatm.find_datm_around(sid, start_ts, tolerance) d
		HAVING count(*) > 0
		UNION
		SELECT (solardatm.calc_datm_at(d, end_ts)).*
		FROM solardatm.find_datm_around(sid, end_ts, tolerance) d
		HAVING count(*) > 0
	)
	-- combine raw datm with reset datm
	SELECT d.stream_id
		, d.ts
		, d.data_i
		, d.data_a
		, d.data_s
		, d.data_t
		, 0::SMALLINT AS rtype
	FROM d
$$;
