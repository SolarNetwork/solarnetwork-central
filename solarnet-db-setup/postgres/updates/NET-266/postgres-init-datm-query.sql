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
 * Project the values of a datum stream at a specific point in time, by deriving from the previous
 * and next values from the same stream.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts` column will
 * simply be `reading_ts`. The `jdata_i` column will be computed as an average of the previous/next rows,
 * and `jdata_a` will be time-projected based on the previous/next readings.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param reading_ts	the timestamp to calculate the value of each datum at
 * @param span			a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
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
