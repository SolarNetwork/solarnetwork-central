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
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
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
	WITH d1 AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
		FROM solardatum.da_datum d 
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts <= ts_min
			AND d.ts > ts_min - tolerance
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	, d2 AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.ts, d.node_id, d.source_id, d.jdata_a
		FROM solardatum.da_datum d
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts <= ts_max
			AND d.ts > ts_max - tolerance
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	, d AS (
		SELECT * FROM d1
		UNION
		SELECT * FROM d2
	)
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
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
