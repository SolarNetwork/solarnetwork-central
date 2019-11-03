/**
 * FUNCTION solardatum.find_most_recent(bigint[], text[])
 * 
 * Find the highest available data for all source IDs for the given node IDs. This query relies on
 * the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 */
CREATE OR REPLACE FUNCTION solardatum.find_most_recent(nodes bigint[], sources text[])
RETURNS SETOF solardatum.da_datum_data LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.*
	FROM  solardatum.da_datum_range mr
	INNER JOIN solardatum.da_datum_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts = mr.ts_max
	WHERE mr.node_id = ANY(nodes) AND mr.source_id = ANY(sources)
	ORDER BY d.node_id, d.source_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_most_recent_hourly(nodes bigint, sources text[]);

/**
 * FUNCTION solardatum.find_most_recent_hourly(bigint[], text[])
 * 
 * Find the highest available hourly data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_hourly(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_hourly_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	INNER JOIN solaragg.agg_datum_hourly_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts_start = date_trunc('hour', mr.ts_max)
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_most_recent_daily(nodes bigint, sources text[]);

/**
 * FUNCTION solardatum.find_most_recent_daily(bigint[], text[])
 * 
 * Find the highest available daily data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_daily(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_daily_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = mr.node_id
	INNER JOIN solaragg.agg_datum_daily_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id 
		AND d.ts_start = date_trunc('day', mr.ts_max AT TIME ZONE nlt.time_zone) AT TIME ZONE nlt.time_zone
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;

DROP FUNCTION IF EXISTS solaragg.find_most_recent_monthly(nodes bigint, sources text[]);

/**
 * FUNCTION solardatum.find_most_recent_monthly(bigint[], text[])
 * 
 * Find the highest available monthly data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_monthly(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_daily_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = mr.node_id
	INNER JOIN solaragg.agg_datum_monthly_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id 
		AND d.ts_start = date_trunc('month', mr.ts_max AT TIME ZONE nlt.time_zone) AT TIME ZONE nlt.time_zone
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;
