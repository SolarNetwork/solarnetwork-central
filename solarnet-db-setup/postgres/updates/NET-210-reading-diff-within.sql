/**
 * Calculate the difference between the accumulating properties of datum within a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and 
 * `ts_end` columns will be the timestamps of the found starting/ending datum records. The `jdata_a`
 * column will be computed as the difference between the starting/ending rows, using the 
 `solarcommon.jsonb_diffsum_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
 *
 * This query will **not** use rows from outside the given date range. It will only use the
 * earliest on or after the `ts_min` date and the latest before or on the `ts_max` date.
 *
 * This query is optimized for **small* time spans, on the order of hours. It queries the 
 * `solardatum.da_datum` table directly, so the larger the time range the longer the query can take.
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
