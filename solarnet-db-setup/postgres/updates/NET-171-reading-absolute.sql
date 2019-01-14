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
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (inclusive)
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	WITH latest_before_start AS (
		SELECT d.ts, d.node_id, d.source_id, d.jdata_a
		FROM  solardatum.find_latest_before(nodes, sources, ts_min) dates
		INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
	)
	, earliest_after_start AS (
		SELECT d.ts, d.node_id, d.source_id, d.jdata_a
		FROM solardatum.find_earliest_after(nodes, sources, ts_min) dates
		INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
	)
	, latest_before_end AS (
		SELECT d.ts, d.node_id, d.source_id, d.jdata_a
		FROM solardatum.find_latest_before(nodes, sources, ts_max) dates
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
		min(nlt.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diff_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM d
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;
