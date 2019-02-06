/**
 * Find the latest available data before a specific point in time.
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
	-- first find max day quickly for each node+source
	WITH max_dates AS (
		SELECT max(ts_start) AS ts_start, node_id, source_id
		FROM solaragg.agg_datum_daily
		WHERE node_id = ANY(nodes)
			AND source_id = ANY(sources)
			AND ts_start < ts_max
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
	GROUP BY d.node_id, d.source_id
$$;

/**
 * Find the earliest available data after a specific point in time.
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
	-- first find min day quickly for each node+source
	WITH min_dates AS (
		SELECT min(ts_start) AS ts_start, node_id, source_id
		FROM solaragg.agg_datum_daily
		WHERE node_id = ANY(nodes)
			AND source_id = ANY(sources)
			AND ts_start >= ts_min
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

/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 * 
 * This function makes use of `solardatum.find_latest_before()` and `solardatum.find_earliest_after`
 * and thus has the same restrictions as those, and as a consequence will consider all data before
 * the given `ts_max` as possible data.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param ts_min		the timestamp of the start of the time range (inclusive), in node local time
 * @param ts_max		the timestamp of the end of the time range (inclusive), in node local time
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	WITH tz AS (
		SELECT nlt.time_zone,
			ts_min AT TIME ZONE nlt.time_zone AS sdate,
			ts_max AT TIME ZONE nlt.time_zone AS edate,
			array_agg(DISTINCT nlt.node_id) AS nodes,
			array_agg(DISTINCT s.source_id) AS sources
		FROM solarnet.node_local_time nlt
		CROSS JOIN (
			SELECT unnest(sources) AS source_id
		) s
		WHERE nlt.node_id = ANY(nodes)
		GROUP BY nlt.time_zone
	)
	, latest_before_start AS (
		SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
		FROM tz
		INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
		INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
	)
	, earliest_after_start AS (
		SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
		FROM tz
		INNER JOIN solardatum.find_earliest_after(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
		INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
	)
	, latest_before_end AS (
		SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
		FROM tz
		INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.edate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
		INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
	)
	, d AS (
		SELECT * FROM (
			SELECT DISTINCT ON (d.node_id, d.source_id) d.*
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.node_id, d.source_id, d.ts
		) earliest
		UNION 
		SELECT * FROM latest_before_end
	)
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(d.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diff_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM d
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;
