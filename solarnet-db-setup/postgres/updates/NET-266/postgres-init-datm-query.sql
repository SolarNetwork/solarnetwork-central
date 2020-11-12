/**
 * Find the datum with the smallest timestamp for a given stream, i.e. the "first" datum in a stream.
 *
 * Using this function can force the fastest index lookup for a single stream, when multiple streams
 * are being queried.
 *
 * @param sid the stream ID
 * @see the `solardatm.find_time_range(uuid[])` function
 */
CREATE OR REPLACE FUNCTION solardatm.find_time_least(sid uuid)
	RETURNS solardatm.da_datm LANGUAGE sql STABLE AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts
	LIMIT 1
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
CREATE OR REPLACE FUNCTION solardatm.find_time_greatest(sid uuid)
	RETURNS solardatm.da_datm LANGUAGE sql STABLE AS
$$
	SELECT *
	FROM solardatm.da_datm
	WHERE stream_id = sid
	ORDER BY ts DESC
	LIMIT 1
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
	RETURNS SETOF solardatm.da_datm LANGUAGE sql ROWS 200 STABLE AS
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
	) RETURNS TABLE (
		stream_id 	UUID,
		ts			TIMESTAMP WITH TIME ZONE,
		data_i		NUMERIC[],
		data_a		NUMERIC[],
		data_s		TEXT[],
		data_t		TEXT[],
		stat_i		NUMERIC[][]
	) LANGUAGE SQL STABLE ROWS 1 AS
$$
	WITH d AS (
		SELECT *
			, CASE
				WHEN count(*) OVER win < 2 THEN 1
				ELSE
					(EXTRACT('epoch' FROM ts_at - first_value(ts) OVER win)
					/ EXTRACT('epoch' FROM last_value(ts) OVER win - first_value(ts) OVER win))
				END AS portion
		FROM solardatm.find_datum_around(sid, ts_at)
		WINDOW win AS (ORDER BY ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	)
	-- calculate instantaneous statistis per property
	, di AS (
		SELECT
			  p.idx
			, to_char(sum(val) * min(portion), 'FM999999999999999999990.999999999')::numeric AS val
			, count(p.val) AS cnt
			, min(p.val) AS val_min
			, max(p.val) AS val_max
		FROM d
		INNER JOIN unnest(d.data_i) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		GROUP BY p.idx
	)
	-- join data_i and stat_i property values back into arrays
	, di_ary AS (
		SELECT
			   array_agg(val ORDER BY idx) AS data_i
			, array_agg(
				ARRAY[cnt, val_min, val_max] ORDER BY idx
			) AS stat_i
		FROM di
	)
	-- calculate accumulating values per property
	, da AS (
		SELECT
			  p.idx
			, to_char(solarcommon.first(val ORDER BY ts) +
				(solarcommon.first(val ORDER BY ts DESC) - solarcommon.first(val ORDER BY ts)) * min(portion)
				, 'FM999999999999999999990.999999999')::numeric AS val
		FROM d
		INNER JOIN unnest(d.data_a) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		GROUP BY p.idx
	)
	-- join data_a property values back into arrays
	, da_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_a
		FROM da
	)
	-- calculate status statistics, as most-frequent status values
	, ds AS (
		SELECT
			  p.idx
			, mode() WITHIN GROUP (ORDER BY p.val) AS val
		FROM d
		INNER JOIN unnest(d.data_s) WITH ORDINALITY AS p(val, idx) ON TRUE
		WHERE p.val IS NOT NULL
		GROUP BY p.idx
	)
	-- join data_s property values back into arrays
	, ds_ary AS (
		SELECT
			  array_agg(val ORDER BY idx) AS data_s
		FROM ds
	)
	-- join data_t property values into mega array
	, dt_ary AS (
		SELECT
			  array_agg(p.val ORDER BY d.ts) AS data_t
		FROM d
		INNER JOIN unnest(d.data_t) AS p(val) ON TRUE
		WHERE d.data_t IS NOT NULL
	)
	SELECT
		sid AS stream_id
		, ts_at AS ts
		, di_ary.data_i
		, da_ary.data_a
		, ds_ary.data_s
		, dt_ary.data_t
		, di_ary.stat_i
	FROM di_ary, da_ary, ds_ary, dt_ary
	WHERE data_i IS NOT NULL OR data_a IS NOT NULL OR data_s IS NOT NULL OR data_t IS NOT NULL
$$;
