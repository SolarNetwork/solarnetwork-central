/**
 * Find the latest available data before a specific point in time.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This function returns at most one row per node+source combination. It also relies
 * on the `solaragg.agg_datum_daily` table to perform an initial narrowing of all possible
 * datum to a single day (for each node+source combination). This means that the query
 * results depend on aggregate data processing to populate the `solaragg.agg_datum_daily`
 * table in order for results to be returned.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_max		the maximum timestamp to search for (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.find_latest_before(
	nodes bigint[], sources text[], ts_max timestamptz)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id character varying(64)
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT ts_start AS sdate, node_ids AS nodes, source_ids AS sources
		FROM solarnet.node_source_time_rounded(nodes, sources, 'day', ts_max)
	)
	-- first find max day quickly for each node+source
	, max_dates AS (
		SELECT max(a.ts_start) AS ts_start, a.node_id, a.source_id
		FROM tz
		INNER JOIN solaragg.agg_datum_daily a ON a.node_id = ANY(tz.nodes) AND a.source_id = ANY(tz.sources)
		WHERE a.ts_start < tz.sdate
			-- including a.jdata_ad here results in index scan vs index only scan so performance is
			-- worse; in Postgres 11 covering index could help here with functional (jdata_ad IS NOT NULL)::bool
			AND a.jdata_ad IS NOT NULL
		GROUP BY node_id, source_id
	)
	, -- then group by day (start of day), so we can batch day+node+source queries together
	max_date_groups AS (
		SELECT ts_start AS ts_start, array_agg(DISTINCT node_id) AS nodes, array_agg(DISTINCT source_id) AS sources
		FROM max_dates
		GROUP BY ts_start
	)
	-- now for each day+node+source find maximum exact date
	SELECT max(d.ts) AS ts, d.node_id, d.source_id
	FROM max_date_groups mdg
	INNER JOIN solardatum.da_datum d ON d.node_id = ANY(mdg.nodes) AND d.source_id = ANY(mdg.sources)
	WHERE d.ts >= mdg.ts_start
		AND d.ts <= ts_max
		AND d.jdata_a IS NOT NULL
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Find the earliest available data after a specific point in time.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This function returns at most one row per node+source combination. It also relies
 * on the `solaragg.agg_datum_daily` table to perform an initial narrowing of all possible
 * datum to a single day (for each node+source combination). This means that the query
 * results depend on aggregate data processing to populate the `solaragg.agg_datum_daily`
 * table in order for results to be returned.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the minimum timestamp to search for (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.find_earliest_after(
	nodes bigint[], sources text[], ts_min timestamptz)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id character varying(64)
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT ts_start AS sdate, node_ids AS nodes, source_ids AS sources
		FROM solarnet.node_source_time_rounded(nodes, sources, 'day', ts_min)
	)
	-- first find min day quickly for each node+source
	, min_dates AS (
		SELECT min(a.ts_start) AS ts_start, a.node_id, a.source_id
		FROM tz
		INNER JOIN solaragg.agg_datum_daily a ON a.node_id = ANY(tz.nodes) AND a.source_id = ANY(tz.sources)
		WHERE a.ts_start >= tz.sdate
			-- including a.jdata_ad here results in index scan vs index only scan so performance is
			-- worse; in Postgres 11 covering index could help here with functional (jdata_ad IS NOT NULL)::bool
			AND a.jdata_ad IS NOT NULL
		GROUP BY node_id, source_id
	)
	, -- then group by day (start of day), so we can batch day+node+source queries together
	min_date_groups AS (
		SELECT ts_start AS ts_start, array_agg(DISTINCT node_id) AS nodes, array_agg(DISTINCT source_id) AS sources
		FROM min_dates
		GROUP BY ts_start
	)
	-- now for each day+node+source find minimum exact date
	SELECT min(d.ts) AS ts, d.node_id, d.source_id
	FROM min_date_groups mdg
	INNER JOIN solardatum.da_datum d ON d.node_id = ANY(mdg.nodes) AND d.source_id = ANY(mdg.sources)
	WHERE d.ts >= ts_min
		AND d.ts <= mdg.ts_start + interval '1 day'
		AND d.jdata_a IS NOT NULL
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum between a time range.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range
 * @param ts_max		the timestamp of the end of the time range
 * @param tolerance		a maximum range before `ts_min` to consider when looking for the datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '1 month')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	WITH latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d 
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_min
					AND d.ts > ts_min - tolerance
					AND d.jdata_a IS NOT NULL
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_min
					AND aux.ts > ts_min - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_max
					AND d.ts > ts_max - tolerance
					AND d.jdata_a IS NOT NULL
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_max
					AND aux.ts > ts_max - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM latest_before_start
		UNION
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(nlt.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum between a time range.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range
 * @param ts_max		the timestamp of the end of the time range
 * @param tolerance		a maximum range before `ts_min` to consider when looking for the datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp, tolerance interval default interval '1 month')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start/end dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT time_zone, ts_start AS sdate, ts_end AS edate, node_ids AS nodes, source_ids AS sources
		FROM solarnet.node_source_time_ranges_local(nodes, sources, ts_min, ts_max)
	)
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	, latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
				WHERE d.node_id = ANY(tz.nodes)
					AND d.source_id = ANY(tz.sources)
					AND d.ts <= tz.sdate
					AND d.ts > tz.sdate - tolerance
					AND d.jdata_a IS NOT NULL
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.sdate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
				WHERE d.node_id = ANY(tz.nodes)
					AND d.source_id = ANY(tz.sources)
					AND d.ts <= tz.edate
					AND d.ts > tz.edate - tolerance
					AND d.jdata_a IS NOT NULL
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts < tz.edate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM latest_before_start
		UNION
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT time_zone
			, node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY time_zone, node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT ranges.time_zone
			, aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(d.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This returns at most one row. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diffsum_jdata()` aggregate function.
 *
 * @param node 			the node ID to find
 * @param source 		the source ID to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (exclusive)
 * @param tolerance 	the maximum time span to look backwards for the previous reading record; smaller == faster
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	node bigint, source text, ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '3 months')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata jsonb
) LANGUAGE sql STABLE ROWS 1 AS $$
	WITH latest_before_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
					AND jdata_a IS NOT NULL
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION ALL
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, earliest_after_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find earliest on/after
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
					AND jdata_a IS NOT NULL
				ORDER BY ts 
				LIMIT 1
			)
			UNION ALL
			(
				-- find earliest on/after reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
				ORDER BY ts
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts, rr DESC
		LIMIT 1
	)
	, latest_before_end AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
					AND jdata_a IS NOT NULL
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION ALL
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_af AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, d AS (
		(
			SELECT *
			FROM (
				SELECT * FROM latest_before_start
				UNION ALL
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.ts
			LIMIT 1
		)
		UNION ALL
		(
			SELECT * FROM latest_before_end
		)
	)
	, ranges AS (
		SELECT min(ts) AS sdate
			, max(ts) AS edate
		FROM d
	)
	, combined AS (
		SELECT * FROM d
	
		UNION ALL
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges, solardatum.da_datum_aux aux 
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
			AND aux.node_id = node 
			AND aux.source_id = source
			AND aux.ts > ranges.sdate
			AND aux.ts < ranges.edate
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_jdata(d.jdata_a ORDER BY d.ts) AS jdata
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum within a time range.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and 
 * `ts_end` columns will be the timestamps of the found starting/ending datum records. The `jdata_a`
 * column will be computed as the difference between the starting/ending rows, using the 
 * `solarcommon.jsonb_diffsum_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * This query will **not** use rows from outside the given date range. It will only use the
 * earliest on or after the `ts_min` date and the latest before or on the `ts_max` date.
 *
 * This query is optimized for time ranges of 24 hours or less.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within_close(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	WITH d AS (
		SELECT d.node_id
			, d.source_id
			, unnest(ARRAY[min(d.ts), max(d.ts)]) AS ts
			, unnest(ARRAY[solarcommon.first(d.jdata_a ORDER BY d.ts), solarcommon.first(d.jdata_a ORDER BY d.ts DESC)]) AS jdata_a
		FROM solardatum.da_datum d
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts >= ts_min
			AND d.ts <= ts_max
			AND d.jdata_a IS NOT NULL
		GROUP BY d.node_id, d.source_id
	)
	, resets AS (
		SELECT aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.ts - interval '1 millisecond', aux.ts]) AS ts
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM solardatum.da_datum_aux aux 
		WHERE aux.node_id = ANY(nodes)
			AND aux.source_id = ANY(sources)
			AND aux.ts >= ts_min
			AND aux.ts <= ts_max
			AND aux.atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION ALL
		SELECT * FROM resets
	)
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	HAVING count(jdata_a) > 1
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum within a time range.
 *
 * NOTE this query only considers rows with accumulating data, i.e. non-NULL `jdata_a` values.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and
 * `ts_end` columns will * the timestamps of the found starting/ending datum records. The `jdata_a`
 * column will be computed as the difference * between the starting/ending rows, using the
 * `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * This query will **not** use rows from outside the given date range. It will only use the
 * earliest on or after the `ts_min` date and the latest before or on the `ts_max` date.
 *
 * This query is optimized for time ranges of 24 hours or less.
 * 
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive), in node local time
 * @param ts_max		the timestamp of the end of the time range (inclusive), in node local time
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within_local_close(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- generate rows of nodes grouped by time zone, get absolute start/end dates for all nodes
	-- but grouped into as few rows as possible to minimize subsequent query times
	WITH tz AS (
		SELECT time_zone, ts_start AS sdate, ts_end AS edate, node_ids AS nodes, source_ids AS sources
		FROM solarnet.node_source_time_ranges_local(nodes, sources, ts_min, ts_max)
	)
	-- query for raw first/last data using absolute date groups found in tz
	, d AS (
		SELECT d.node_id
			, d.source_id
			, unnest(ARRAY[min(d.ts), max(d.ts)]) AS ts
			, unnest(ARRAY[solarcommon.first(d.jdata_a ORDER BY d.ts), solarcommon.first(d.jdata_a ORDER BY d.ts DESC)]) AS jdata_a
		FROM tz
		INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
		WHERE d.ts >= tz.sdate
			AND d.ts <= tz.edate
			AND d.jdata_a IS NOT NULL
		GROUP BY d.node_id, d.source_id
	)
	, resets AS (
		SELECT aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.ts - interval '1 millisecond', aux.ts]) AS ts
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM solardatum.da_datum_aux aux 
		WHERE aux.node_id = ANY(nodes)
			AND aux.source_id = ANY(sources)
			AND aux.ts >= ts_min
			AND aux.ts <= ts_max
			AND aux.atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION ALL
		SELECT * FROM resets
	)
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	HAVING count(jdata_a) > 1
	ORDER BY d.node_id, d.source_id
$$;

/**
 * Index da_datum_x_acc_idx
 *
 * This index is designed to support the `solardatum.calculate_datum_diff_over(
 * 	 node bigint, source text, ts_min timestamptz, ts_max timestamptz, tolerance interval)`
 * function, which is called by the `solaragg.process_one_agg_stale_datum(kind character)`
 * function during aggregate datum processing. The queries need to find the most/least recent
 * data where `jdata_a` is not NULL, thus this index is defined as a partial index with
 * `WHERE jdata_a IS NOT NULL`. The `jdata_a` column is included in the index ONLY so that
 * an index-only scan can be performed, as the query only needs this column in the result.
 * In Postgres 11 a covering index could be used instead.
 *
 * Note the name includes `_x` because scripts that cluster the table sort the indexes by
 * name and use the one that sorts first; thus this addition sorts the index later.
 */
CREATE UNIQUE INDEX da_datum_x_acc_idx ON solardatum.da_datum (node_id, source_id, ts DESC, jdata_a)
    WHERE jdata_a IS NOT NULL;


-- update to use EXECUTE for some queries, which is resulting in dramatically faster query plans
CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					record;
	stale_t_start			timestamp;
	stale_t_end 			timestamp;
	stale_ts_prevstart		timestamptz;
	stale_ts_end 			timestamptz;
	agg_span 				interval;
	agg_json 				jsonb := NULL;
	agg_jmeta 				jsonb := NULL;
	agg_reading 			jsonb := NULL;
	agg_reading_ts_start 	timestamptz := NULL;
	agg_reading_ts_end 		timestamptz := NULL;
	node_tz 				text := 'UTC';
	proc_count 				integer := 0;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = kind
		-- Too slow to order; not strictly fair but process much faster
		-- ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
		LIMIT 1
		FOR UPDATE SKIP LOCKED;
BEGIN
	CASE kind
		WHEN 'h' THEN
			agg_span := interval '1 hour';
		WHEN 'd' THEN
			agg_span := interval '1 day';
		ELSE
			agg_span := interval '1 month';
	END CASE;

	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get the node TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = stale.node_id
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.node_id;
			node_tz := 'UTC';
		END IF;
		
		-- stash local time start/end so date calculations for day+ correctly handles DST boundaries
		stale_t_start := stale.ts_start AT TIME ZONE node_tz;
		stale_t_end := stale_t_start + agg_span;
		stale_ts_prevstart := (stale_t_start - agg_span) AT TIME ZONE node_tz;
		stale_ts_end := stale_t_end AT TIME ZONE node_tz;

		CASE kind
			WHEN 'h' THEN
				-- Dramatically faster execution via EXECUTE than embedded SQL here; better query plan
				
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_datum_time_slots($1, $2, $3, $4, $5, $6)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour';
				
				EXECUTE 'SELECT jdata, ts_start, ts_end FROM solardatum.calculate_datum_diff_over($1, $2, $3, $4)'
				INTO agg_reading, agg_reading_ts_start, agg_reading_ts_end
				USING stale.node_id, stale.source_id::text, stale.ts_start, stale.ts_start + agg_span;

			WHEN 'd' THEN
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'h';
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;

			ELSE
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'd';
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;
		END CASE;

		IF agg_json IS NULL AND (agg_reading IS NULL 
				OR (agg_reading_ts_start IS NOT NULL AND agg_reading_ts_start = agg_reading_ts_end)
				) THEN
			-- no data in range, so delete agg row
			-- using date range in case time zone of node has changed
			CASE kind
				WHEN 'h' THEN
					-- note NOT using stale_ts_prevstart here because not needed for hourly
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_datum_hourly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						stale_t_start,
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- no delete from node tz change needed for hourly
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_datum_daily (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
				ELSE
					INSERT INTO solaragg.agg_datum_monthly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;
		proc_count := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		-- and also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;

			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'r')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;
		END CASE;
	END IF;
	CLOSE curs;
	RETURN proc_count;
END;
$$;
