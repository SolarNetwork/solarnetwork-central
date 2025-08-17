/**
 * Find the datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 * @see the `solardatm.find_time_range(uuid[])` function
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_least(sid UUID)
	RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts
	LIMIT 1
$$;


/**
 * Find the datum with the smallest timestamp for a set of streams, i.e. the "first" datum in each
 * stream.
 *
 * @param sids the stream IDs to search for
 * @see solardatm.find_time_least(uuid)
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_least(sids UUID[])
	RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE AS
$$
	SELECT d.*
	FROM unnest(sids) ids(stream_id)
	INNER JOIN solardatm.find_time_least(ids.stream_id) d ON TRUE
$$;


/**
 * Find the datum with the largest timestamp for a given stream, i.e. the "last" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 * @see the `solardatm.find_time_range(uuid[])` function
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_greatest(sid UUID)
	RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts DESC
	LIMIT 1
$$;


/**
 * Find the datum with the largest timestamp for a set of streams, i.e. the "last" datum in each
 * stream.
 *
 * @param sids the stream IDs to search for
 * @see solardatm.find_time_greatest(uuid)
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_greatest(sids UUID[])
	RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE AS
$$
	SELECT d.*
	FROM unnest(sids) ids(stream_id)
	INNER JOIN solardatm.find_time_greatest(ids.stream_id) d ON TRUE
$$;


/**
 * Find the smallest and largest datum for a given stream, i.e. the "first" and "last".
 *
 * This will return two rows for each stream that has any datm available, even if there is only
 * one datm in the stream.
 *
 * @param stream_ids the stream IDs to return results for
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_range(stream_ids uuid[])
	RETURNS SETOF solardatm.da_datm LANGUAGE SQL ROWS 200 STABLE AS
$$
	WITH ids AS (
		SELECT unnest(stream_ids) AS stream_id
	)
	, d AS (
		(
		SELECT d.*
		FROM ids
		INNER JOIN solardatm.find_time_least(ids.stream_id) d ON d.stream_id = ids.stream_id
		)
		UNION ALL
		(
		SELECT d.*
		FROM ids
		INNER JOIN solardatm.find_time_greatest(ids.stream_id) d ON d.stream_id = ids.stream_id
		)
	)
	SELECT * FROM d
$$;


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
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts < ts_at
		AND ts >= cutoff
		AND NOT(must_a AND (has_no_a OR data_a IS NULL))
	ORDER BY ts DESC
	LIMIT 1;
END
$$;

/**
 * Find a datum time immediately earlier in time a given instance, within a cutoff.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 * @param cutoff 			the maximum time to look backward for adjacent datm
 */
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
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY
	SELECT ts
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts > ts_at
		AND ts <= cutoff
		AND NOT(must_a AND (has_no_a OR data_a IS NULL))
	ORDER BY ts
	LIMIT 1;
END
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
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_after(
	sid UUID,
	ts_at TIMESTAMP WITH TIME ZONE,
	cutoff TIMESTAMP WITH TIME ZONE
) RETURNS SETOF TIMESTAMP WITH TIME ZONE LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT * FROM solardatm.find_time_after_ts(sid, ts_at, cutoff, FALSE, FALSE);
$$;


/**
 * Find if a datum has accumualting properties at a specific timestamp.
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param ts_at				the date of the datum to find adjacent datm for
 */
CREATE OR REPLACE FUNCTION solardatm.datm_has_accumulating_at(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE
	) RETURNS TABLE (
		ts 			TIMESTAMP WITH TIME ZONE,
		has_a 		BOOLEAN
	) LANGUAGE plpgsql STABLE ROWS 1 AS
$$
BEGIN
	RETURN QUERY
	SELECT d.ts, COALESCE(CARDINALITY(d.data_a) > 0, FALSE) AS has_a
	FROM solardatm.da_datm d
	WHERE d.stream_id = sid
	AND d.ts = ts_at;
END
$$;


/**
 * Find datum between two dates, like BETWEEN (inclusive start and end).
 *
 * This function can be used for performance reasons in other functions, to force the query planner
 * to use a full date constraint in the query.
 *
 * @param sid 				the stream ID of the datm stream to search
 * @param start_ts			the minimum date (inclusive)
 * @param end_ts 			the maximum date (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_between(
	sid 		UUID,
	start_ts 	TIMESTAMP WITH TIME ZONE,
	end_ts 		TIMESTAMP WITH TIME ZONE
) RETURNS SETOF solardatm.da_datm LANGUAGE plpgsql STABLE ROWS 500 AS
$$
BEGIN
	RETURN QUERY
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
		AND ts >= start_ts
		AND ts <= end_ts;
END
$$;


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
				-- but then join back to solardatm.da_datm to restrict on must_a
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
					WHERE d.has_a = TRUE
				)
				, t AS (
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
					) AS ts
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
 * @see solardatm.find_datm_around_ts
 */
CREATE OR REPLACE FUNCTION solardatm.find_datm_around(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '1 months',
		must_a		BOOLEAN DEFAULT FALSE
	) RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 2 AS
$$
	SELECT * FROM solardatm.find_datm_around_ts(sid, ts_at, tolerance, must_a, FALSE);
$$;


/**
 * Project the values of a datum stream at a specific point in time, by deriving from the previous
 * and next values from the same stream.
 *
 * This returns at most one row.
 *
 * @param sid 			the stream ID to find
 * @param ts_at			the timestamp to calculate the value of each datum at
 * @param tolerance		a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
 * @see solardatm.calc_datm_at(datum, timestamp)
 */
CREATE OR REPLACE FUNCTION solardatm.calc_datm_at(
		sid 		UUID,
		ts_at 		TIMESTAMP WITH TIME ZONE,
		tolerance 	INTERVAL DEFAULT interval '1 months'
	) RETURNS SETOF solardatm.da_datm LANGUAGE SQL STABLE ROWS 1 AS
$$
	SELECT (solardatm.calc_datm_at(d, ts_at)).*
	FROM solardatm.find_datm_around(sid, ts_at, tolerance) d
	HAVING count(*) > 0
$$;
