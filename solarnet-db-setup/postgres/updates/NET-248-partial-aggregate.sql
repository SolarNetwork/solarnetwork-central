/**
 * Calculate the minimum number of absolute rounded date ranges for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute dates. This function depends on the `solardatum.da_datum_range` table
 * to filter out node+source combinations that don't exist.
 * 
 * @param nodes     the list of nodes to resolve absolute dates for
 * @param sources   a list of source IDs to include in the results (optional)
 * @param field     the Postgres date_trunc compatible field to truncate the date on, e.g. 'hour', 'day', 'month', etc.
 * @param min_date  the start date to truncate
 * @param max_date  the end date to truncate
 */
CREATE OR REPLACE FUNCTION solardatum.node_source_time_rounded(
	nodes bigint[], sources text[], field text , min_date timestamp, max_date timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT 
		date_trunc(field, min_date) AT TIME ZONE nlt.time_zone AS ts_start
		, date_trunc(field, max_date) AT TIME ZONE nlt.time_zone AS ts_end
		, nlt.time_zone AS time_zone
		, array_agg(DISTINCT dr.node_id) AS node_ids
		, array_agg(DISTINCT dr.source_id) FILTER (WHERE dr.source_id IS NOT NULL) AS source_ids
	FROM solardatum.da_datum_range dr 
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = dr.node_id
	WHERE dr.node_id = ANY(nodes)
	AND dr.source_id = ANY(sources)
	GROUP BY time_zone;
$$;
