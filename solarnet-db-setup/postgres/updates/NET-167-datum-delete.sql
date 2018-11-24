/**
 * Cast a JSON array to a BIGINT array.
 *
 * @param json the JSON array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.json_array_to_bigint_array(json)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM json_array_elements_text($1) t(x);
$$;

/**
 * Cast a JSONB array to a BIGINT array.
 *
 * @param jsonb the JSONB array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_array_to_bigint_array(jsonb)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM jsonb_array_elements_text($1) t(x);
$$;

/**
 * Calculate the minimum number of absolute time spans required for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute start/end dates.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_ranges_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT ts_min AT TIME ZONE nlt.time_zone AS sdate,
		ts_max AT TIME ZONE nlt.time_zone AS edate,
		nlt.time_zone AS time_zone,
		array_agg(DISTINCT nlt.node_id) AS nodes,
		array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone
$$;

/**
 * Calculate the count of rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatum.datum_record_counts(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
	query_date timestamptz, 
	datum_count bigint, 
	datum_hourly_count integer, 
	datum_daily_count integer, 
	datum_monthly_count integer
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
BEGIN
	-- raw count
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solardatum.da_datum d, nlt
	WHERE 
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_count;

	-- count hourly data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_hourly d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('hour', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_hourly_count;

	-- count daily data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_daily d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('day', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_daily_count;

	-- count daily data
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	SELECT count(*)
	FROM solaragg.agg_datum_monthly d, nlt
	WHERE 
		d.ts_start >= nlt.ts_start
		AND d.ts_start < date_trunc('month', nlt.ts_end)
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids))
	INTO datum_monthly_count;

	query_date = CURRENT_TIMESTAMP;
	RETURN NEXT;
END
$$;

/**
 * Calculate the count of rows matching a set of nodes, sources, and a local date range.
 *
 * The `jfilter` parameter must provide the following items:
 * 
 * * `nodeIds` - array of node IDs
 * * `sourceIds` - (optional) array of source IDs
 * * `localStartDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * * `localEndDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solardatum.datum_record_counts_for_filter(jfilter jsonb)
RETURNS TABLE(
	query_date timestamptz, 
	datum_count bigint, 
	datum_hourly_count integer, 
	datum_daily_count integer, 
	datum_monthly_count integer
) LANGUAGE plpgsql STABLE AS $$
DECLARE
	node_ids bigint[] := solarcommon.jsonb_array_to_bigint_array(jfilter->'nodeIds');
	source_ids text[] := solarcommon.json_array_to_text_array(jfilter->'sourceIds');
	ts_min timestamp := jfilter->>'localStartDate';
	ts_max timestamp := jfilter->>'localEndDate';
BEGIN
	RETURN QUERY
	SELECT * FROM solardatum.datum_record_counts(node_ids, source_ids, ts_min, ts_max);
END
$$;

/**
 * Delete datum rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatum.delete_datum(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp)
RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	all_source_ids boolean := sources IS NULL OR array_length(sources, 1) < 1;
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
BEGIN
	WITH nlt AS (
		SELECT time_zone, ts_start, ts_end, node_ids, source_ids
		FROM solarnet.node_source_time_ranges_local(nodes, sources, start_date, end_date)
	)
	DELETE FROM solardatum.da_datum d
	USING nlt
	WHERE 
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.node_id = ANY(nlt.node_ids)
		AND (all_source_ids OR d.source_id = ANY(nlt.source_ids));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	RETURN total_count;
END
$$;

/**
 * Calculate the count of rows matching a set of nodes, sources, and a local date range.
 *
 * The `jfilter` parameter must provide the following items:
 * 
 * * `nodeIds` - array of node IDs
 * * `sourceIds` - (optional) array of source IDs
 * * `localStartDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * * `localEndDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * 
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date
 * @param ts_max the ending local date
 */
CREATE OR REPLACE FUNCTION solardatum.delete_datum_for_filter(jfilter jsonb)
RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	node_ids bigint[] := solarcommon.jsonb_array_to_bigint_array(jfilter->'nodeIds');
	source_ids text[] := solarcommon.json_array_to_text_array(jfilter->'sourceIds');
	ts_min timestamp := jfilter->>'localStartDate';
	ts_max timestamp := jfilter->>'localEndDate';
	total_count bigint := 0;
BEGIN
	SELECT solardatum.delete_datum(node_ids, source_ids, ts_min, ts_max) INTO total_count;
	RETURN total_count;
END
$$;
