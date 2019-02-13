/**
 * FUNCTION solarnet.node_date_ranges(bigint[], text[], timestamp, timestamp)
 * 
 * Group nodes into absolute date/time ranges based on a local date range translated
 * into the local time zones of each node. The nodes are grouped by time zone so
 * the results will contain one row per time zone, along with the nodes in that time zone.
 * The sources are included without change in each row.
 *
 * This can be useful in CTE clauses to minimize the number of loops a query must make
 * over local date ranges.
 *
 * @param nodes the node IDs to group
 * @param sources the source IDs to include in the results
 * @param ts_min the start of the time range
 * @param ts_max the end of the time range
 */
CREATE OR REPLACE FUNCTION solarnet.node_date_ranges(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  time_zone text,
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  nodes bigint[],
  sources text[]
) LANGUAGE sql STABLE AS $$
	SELECT nlt.time_zone,
		ts_min AT TIME ZONE nlt.time_zone AS ts_start,
		ts_max AT TIME ZONE nlt.time_zone AS ts_end,
		array_agg(DISTINCT nlt.node_id) AS nodes,
		array_agg(DISTINCT s.source_id) AS sources
	FROM solarnet.node_Local_time nlt
	CROSS JOIN (
		SELECT unnest(sources) AS source_id
	) s
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY nlt.time_zone
$$;
