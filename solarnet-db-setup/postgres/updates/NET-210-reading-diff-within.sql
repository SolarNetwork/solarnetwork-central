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
			, unnest(ARRAY[aux.ts - interval '1 millisecond',aux.ts]) AS ts
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
		SELECT unnest(ARRAY[aux.ts - interval '1 millisecond',aux.ts]) AS ts
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
		-- TODO solardatum.calculate_datum_diff_within_far query
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_far(nodes, sources, ts_min, ts_max);
	ELSE
		RETURN QUERY
		SELECT * FROM solardatum.calculate_datum_diff_within_close(nodes, sources, ts_min, ts_max);
	END IF;
END;
$$;

