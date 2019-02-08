-- update existing functions to incorporate da_datum_aux "Reset" record support

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
			UNION
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
			UNION
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
			UNION
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
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(nlt.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


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
			UNION
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
			UNION
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
			UNION
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
				UNION
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
		UNION
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

CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '1 month')
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
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d 
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_min
					AND d.ts > ts_min - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_as AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_min
					AND aux.ts > ts_min - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.da_datum d
				WHERE d.node_id = ANY(nodes)
					AND d.source_id = ANY(sources)
					AND d.ts <= ts_max
					AND d.ts > ts_max - tolerance
				ORDER BY d.node_id, d.source_id, d.ts DESC
			)
			UNION
			(
				SELECT DISTINCT ON (aux.node_id, aux.source_id) aux.ts, aux.node_id, aux.source_id, aux.jdata_af AS jdata_a
				FROM solardatum.da_datum_aux aux
				WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
					AND aux.node_id = ANY(nodes)
					AND aux.source_id = ANY(sources)
					AND aux.ts <= ts_max
					AND aux.ts > ts_max - tolerance
				ORDER BY aux.node_id, aux.source_id, aux.ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM latest_before_start
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
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(nlt.time_zone) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;

CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_local(
	nodes bigint[], sources text[], ts_min timestamp, ts_max timestamp, tolerance interval default interval '1 month')
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
	), d1 AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
		FROM tz
		INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
		WHERE d.node_id = ANY(tz.nodes)
			AND d.source_id = ANY(tz.sources)
			AND d.ts <= tz.sdate
			AND d.ts > tz.sdate - tolerance
		ORDER BY d.node_id, d.source_id, d.ts DESC
	), d2 AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) tz.time_zone, d.ts, d.node_id, d.source_id, d.jdata_a
		FROM tz
		INNER JOIN solardatum.da_datum d ON d.node_id = ANY(tz.nodes) AND d.source_id = ANY(tz.sources)
		WHERE d.node_id = ANY(tz.nodes)
			AND d.source_id = ANY(tz.sources)
			AND d.ts <= tz.edate
			AND d.ts > tz.edate - tolerance
		ORDER BY d.node_id, d.source_id, d.ts DESC
	), d AS (
		SELECT * FROM d1
		UNION
		SELECT * FROM d2
	)
	, ranges AS (
		SELECT time_zone
			, node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY time_zone, node_id, source_id
	)
	, resets AS (
		SELECT ranges.time_zone
			, aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts >= ranges.sdate AND aux.ts <= ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
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
