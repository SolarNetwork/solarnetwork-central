/**
 * Calculate the minimum number of absolute rounded dates for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute dates.
 * 
 * @param nodes   the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param field   the Postgres date_trunc compatible field to truncate the date on, e.g. 'hour', 'day', 'month', etc.
 * @param ts      the date to truncate
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_rounded(
	nodes bigint[], sources text[], field text , ts timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT 
		date_trunc(field, ts AT TIME ZONE nlt.time_zone) AT TIME ZONE nlt.time_zone AS ts_start
		, (date_trunc(field, ts AT TIME ZONE nlt.time_zone) + ('1 '||field)::interval) AT TIME ZONE nlt.time_zone AS ts_end
		, nlt.time_zone AS time_zone
		, array_agg(DISTINCT nlt.node_id) AS nodes
		, array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone
$$;

-- FIX to find earliest date when no data available after given date, but correctly mapping
-- provided date to node-local dates when comparing to agg_datum_daily table
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
	GROUP BY d.node_id, d.source_id
$$;


-- FIX same as above in solardatum.find_earliest_after
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
		SELECT ts_end AS edate, node_ids AS nodes, source_ids AS sources
		FROM solarnet.node_source_time_rounded(nodes, sources, 'day', ts_max)
	)
	-- first find max day quickly for each node+source
	, max_dates AS (
		SELECT max(a.ts_start) AS ts_start, a.node_id, a.source_id
		FROM tz
		INNER JOIN solaragg.agg_datum_daily a ON a.node_id = ANY(tz.nodes) AND a.source_id = ANY(tz.sources)
		WHERE a.ts_start < tz.edate
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
	WHERE d.ts >= mdg.ts_start - interval '1 day'
		AND d.ts <= ts_max
	GROUP BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum within a time range.
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
 * This query is optimized for time ranges greater than 24 hours.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within_far(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	WITH earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_earliest_after(nodes, sources, ts_min) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
				WHERE dates.ts <= ts_max
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts >= ts_min
					AND ts <= ts_max
				ORDER BY node_id, source_id, ts
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts
	)
	-- find records closest to, but not after max date (could be same as earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_latest_before(nodes, sources, ts_max) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
				AND dates.ts >= ts_min
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_af AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts >= ts_min
					AND ts <= ts_max
				ORDER BY node_id, source_id, ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM earliest_after_start
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
		SELECT unnest(ARRAY[aux.ts - interval '1 millisecond', aux.ts]) AS ts
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
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Calculate the difference between the accumulating properties of datum within a time range.
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
 * This function delegates to `solardatum.calculate_datum_diff_within_close` for time ranges of
 * 24 hours or less. Otherwise it delegates to `solardatum.calculate_datum_diff_within_far`.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	tdiff interval := ts_max - ts_min;
BEGIN
	IF tdiff > interval '24 hours' THEN
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_far(nodes, sources, ts_min, ts_max);
	ELSE
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_close(nodes, sources, ts_min, ts_max);
	END IF;
END;
$$;


/**
 * Calculate the difference between the accumulating properties of datum within a time range.
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
 * This query is optimized for time ranges greater than 24 hours.
 * 
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive), in node local time
 * @param ts_max		the timestamp of the end of the time range (inclusive), in node local time
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within_local_far(
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
	-- find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_earliest_after(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
				WHERE dates.ts <= tz.edate

			)
			UNION ALL
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts >= tz.sdate
					AND aux.ts <= tz.edate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.edate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
				WHERE dates.ts >= tz.sdate
			)
			UNION ALL
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts >= tz.sdate
					AND aux.ts <= tz.edate
				ORDER BY tz.time_zone, aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM earliest_after_start
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
			, unnest(ARRAY[aux.ts - interval '1 millisecond', aux.ts]) AS ts
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
		UNION ALL
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
 * Calculate the difference between the accumulating properties of datum within a time range.
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
 * This function delegates to `solardatum.calculate_datum_diff_within_local_close` for time ranges
 * of 24 hours or less. Otherwise it delegates to `solardatum.calculate_datum_diff_within_local_far`.
 * 
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive), in node local time
 * @param ts_max		the timestamp of the end of the time range (inclusive), in node local time
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_within_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	tdiff interval := ts_max - ts_min;
BEGIN
	IF tdiff > interval '24 hours' THEN
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_local_far(nodes, sources, ts_min, ts_max);
	ELSE
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_local_close(nodes, sources, ts_min, ts_max);
	END IF;
END;
$$;
