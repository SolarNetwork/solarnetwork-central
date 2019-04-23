/**
 * FUNCTION solaragg.find_datum_for_time_span(bigint, text[], timestamp with time zone, interval, interval)
 * 
 * Find rows in the <b>solardatum.da_datum</b> table necessary to calculate aggregate
 * data for a specific duration of time, node, and set of sources. This function will return
 * all available rows within the specified duration, possibly with some rows <em>before</em> or
 * <em>after</em> the duration to enable calculating the actual aggregate over the duration.
 *
 * This query will also include `solardatum.da_datum_aux` records of type `Reset`. These records
 * will include a `Reset` tag value, and either `final` or `start` detailing which reset value
 * it represents.
 *
 * @param node The ID of the node to search for.
 * @param sources An array of one or more source IDs to search for, any of which may match.
 * @param start_ts The start time of the desired time duration.
 * @param span The interval of the time duration, which starts from <b>start_ts</b>.
 * @param slotsecs The number of seconds per minute time slot to assign output rows to. Must be
 *                 between 60 and 1800 and evenly divide into 1800.
 * @param tolerance An interval representing the maximum amount of time before between, and after
 *                  rows with the same source ID are allowed to be considered <em>consecutive</em>
 *                  for the purposes of calculating the overall aggregate of the time duration.
 *
 * @out ts The <b>solardatum.da_datum.ts</b> value.
 * @out source_id The <b>solardatum.da_datum.source_id</b> value.
 * @out jdata The <b>solardatum.da_datum.jdata</b> value.
 * @returns one or more records
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_span(
	node bigint,
	sources text[],
	start_ts timestamp with time zone,
	span interval,
	tolerance interval DEFAULT '01:00:00'::interval)
RETURNS TABLE(ts timestamp with time zone, source_id text, jdata jsonb) LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- find raw data with support for filtering out "extra" leading/lagging rows from results
	WITH d AS (
		SELECT
			d.ts,
			d.source_id,
			CASE
				WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win >= (start_ts + span)
					THEN TRUE
				ELSE FALSE
			END AS outside,
			solardatum.jdata_from_datum(d) as jdata,
			0 AS rr
		FROM solardatum.da_datum d
		WHERE d.node_id = node
			AND d.source_id = ANY(sources)
			AND d.ts >= start_ts - tolerance
			AND d.ts <= start_ts + span + tolerance
		WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.source_id
			, CASE
				WHEN lead(aux.ts) over win < start_ts OR lag(aux.ts) over win >= (start_ts + span)
					THEN TRUE
				ELSE FALSE
			END AS outside
			, unnest(ARRAY[solardatum.jdata_from_datum_aux_final(aux), solardatum.jdata_from_datum_aux_start(aux)]) AS jdata
			, 1 AS rr
		FROM solardatum.da_datum_aux aux
		WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
			AND aux.node_id = node
			AND aux.source_id = ANY(sources)
			AND aux.ts >= start_ts - tolerance
			AND aux.ts <= start_ts + span + tolerance
		WINDOW win AS (PARTITION BY aux.source_id ORDER BY aux.ts)
	)
	-- combine raw data with reset pairs
	, combined AS (
		SELECT * FROM d WHERE outside = FALSE
		UNION ALL
		SELECT * FROM resets WHERE outside = FALSE
	)
	-- add order by rr so that when datum & reset have equivalent ts, reset has priority
	SELECT DISTINCT ON (ts, source_id) ts, source_id, jdata
	FROM combined
	ORDER BY ts, source_id, rr DESC
$$;


-- remainder fixes are for UNION ALL




/**
 * Calculate the difference between the accumulating properties of datum over a time range.
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
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
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
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	WITH latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			SELECT d.ts, d.node_id, d.source_id, d.jdata_a
			FROM  solardatum.find_latest_before(nodes, sources, ts_min) dates
			INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			UNION ALL
			SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
			FROM solardatum.da_datum_aux
			WHERE atype = 'Reset'::solardatum.da_datum_aux_type
				AND node_id = ANY(nodes)
				AND source_id = ANY(sources)
				AND ts < ts_min
			ORDER BY node_id, source_id, ts DESC
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- in case no data before min date, find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_earliest_after(nodes, sources, ts_min) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION ALL
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts >= ts_min
				ORDER BY node_id, source_id, ts
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
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_latest_before(nodes, sources, ts_max) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION ALL
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_af AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts < ts_max
				ORDER BY node_id, source_id, ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
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
		UNION ALL
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
		UNION ALL
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
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata_a` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diff_object()` aggregate function.
 *
 * The `solardatum.da_datum_aux` table will be considered, for `Reset` type rows.
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
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_latest_before(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION ALL
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
	-- in case no data before min date, find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
				FROM tz
				INNER JOIN solardatum.find_earliest_after(tz.nodes, tz.sources, tz.sdate) dates ON dates.node_id = ANY(tz.nodes) AND dates.source_id = ANY(tz.sources)
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION ALL
			(
				SELECT DISTINCT ON (tz.time_zone, aux.node_id, aux.source_id)
					tz.time_zone, aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM tz
				INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ANY(tz.nodes) AND aux.source_id = ANY(tz.sources)
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.ts >= tz.sdate
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
			)
			UNION ALL
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
		SELECT * FROM (
			SELECT DISTINCT ON (d.node_id, d.source_id) d.*
			FROM (
				SELECT * FROM latest_before_start
				UNION ALL
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.node_id, d.source_id, d.ts
		) earliest
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
