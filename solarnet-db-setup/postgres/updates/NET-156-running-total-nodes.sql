/**
 * Calculate a running average of datum up to a specific end date. There will
 * be at most one result row per node ID + source ID in the returned data.
 *
 * @param nodes   The IDs of the nodes to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts  An optional date to limit the results to. If not provided the current date is used.
 */
CREATE OR REPLACE FUNCTION solaragg.calc_running_datum_total(
	nodes bigint[],
	sources text[],
	end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	node_id bigint,
	source_id text,
	jdata jsonb)
LANGUAGE sql STABLE ROWS 10 AS
$BODY$
	WITH nodetz AS (
		SELECT nids.node_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT unnest(nodes) AS node_id) AS nids
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = nids.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	)
	SELECT end_ts, end_ts AT TIME ZONE nodetz.tz AS local_date, r.node_id, r.source_id, r.jdata
	FROM nodetz
	CROSS JOIN LATERAL (
		SELECT nodetz.node_id, t.*
		FROM solaragg.calc_running_total(
			nodetz.node_id,
			sources,
			end_ts,
			FALSE) t
	) AS r
$BODY$;

/**
 * Calculate a running average of location datum up to a specific end date. There will
 * be at most one result row per source ID in the returned data.
 *
 * @param locs    The IDs of the locations to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts  An optional date to limit the results to. If not provided the current date is used.
 */
CREATE OR REPLACE FUNCTION solaragg.calc_running_loc_datum_total(
	IN locs bigint[],
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	loc_id bigint,
	source_id text,
	jdata jsonb)
LANGUAGE sql STABLE ROWS 10 AS
$BODY$
	WITH loctz AS (
		SELECT lids.loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT unnest(locs) AS loc_id) AS lids
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = lids.loc_id
	)
	SELECT end_ts, end_ts AT TIME ZONE loctz.tz AS local_date, r.loc_id, r.source_id, r.jdata
	FROM loctz
	CROSS JOIN LATERAL (
		SELECT loctz.loc_id, t.*
		FROM solaragg.calc_running_total(
			loctz.loc_id,
			sources,
			end_ts,
			TRUE) t
	) AS r
$BODY$;

-- Updates to simplify existing datum functions

CREATE OR REPLACE FUNCTION solaragg.find_running_datum(
    IN node bigint,
    IN sources text[],
    IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
  	ts_start timestamp with time zone,
  	local_date timestamp without time zone,
  	node_id bigint,
  	source_id text,
  	jdata jsonb,
  	weight integer)
LANGUAGE sql
STABLE AS
$BODY$
	-- get the node TZ, falling back to UTC if not available so we always have a time zone even if node not found
	WITH nodetz AS (
		SELECT nids.node_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT node AS node_id) nids
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = nids.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	)
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d),
		CAST(extract(epoch from (local_date + interval '1 month') - local_date) / 3600 AS integer) AS weight
	FROM solaragg.agg_datum_monthly d
	INNER JOIN nodetz ON nodetz.node_id = d.node_id
	WHERE d.ts_start < date_trunc('month', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d),
		24::integer as weight
	FROM solaragg.agg_datum_daily d
	INNER JOIN nodetz ON nodetz.node_id = d.node_id
	WHERE ts_start < date_trunc('day', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.ts_start >= date_trunc('month', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, solaragg.jdata_from_datum(d),
		1::INTEGER as weight
	FROM solaragg.agg_datum_hourly d
	INNER JOIN nodetz ON nodetz.node_id = d.node_id
	WHERE d.ts_start < date_trunc('hour', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.ts_start >= date_trunc('day', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT ts_start, ts_start at time zone nodetz.tz AS local_date, nodetz.node_id, source_id, jdata, 1::integer as weight
	FROM solaragg.calc_datum_time_slots(
		node,
		sources,
		date_trunc('hour', end_ts),
		interval '1 hour',
		0,
		interval '1 hour')
	INNER JOIN nodetz ON nodetz.node_id = node
	ORDER BY ts_start, source_id
$BODY$;

CREATE OR REPLACE FUNCTION solaragg.calc_running_datum_total(
	IN node bigint,
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	node_id bigint,
	source_id text,
	jdata jsonb)
LANGUAGE sql
STABLE
ROWS 10 AS
$BODY$
	WITH nodetz AS (
		SELECT nids.node_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT node AS node_id) nids
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = nids.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	)
	SELECT end_ts, end_ts AT TIME ZONE nodetz.tz AS local_date, node, r.source_id, r.jdata
	FROM solaragg.calc_running_total(
		node,
		sources,
		end_ts,
		FALSE
	) AS r
	INNER JOIN nodetz ON nodetz.node_id = node;
$BODY$;

-- Updates to simplify existing loc datum functions

 CREATE OR REPLACE FUNCTION solaragg.find_running_loc_datum(
	IN loc bigint,
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	loc_id bigint,
	source_id text,
	jdata jsonb,
	weight integer)
LANGUAGE sql
STABLE AS
$BODY$
	WITH loctz AS (
		SELECT lids.loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT loc AS loc_id) lids
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = lids.loc_id
	)
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), CAST(extract(epoch from (local_date + interval '1 month') - local_date) / 3600 AS integer) AS weight
	FROM solaragg.agg_loc_datum_monthly d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE d.ts_start < date_trunc('month', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), 24::integer as weight
	FROM solaragg.agg_loc_datum_daily d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE ts_start < date_trunc('day', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.ts_start >= date_trunc('month', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.loc_id, d.source_id, solaragg.jdata_from_datum(d), 1::INTEGER as weight
	FROM solaragg.agg_loc_datum_hourly d
	INNER JOIN loctz ON loctz.loc_id = d.loc_id
	WHERE d.ts_start < date_trunc('hour', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.ts_start >= date_trunc('day', end_ts AT TIME ZONE loctz.tz) AT TIME ZONE loctz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT ts_start, ts_start at time zone loctz.tz AS local_date, loctz.loc_id, source_id, jdata, 1::integer as weight
	FROM solaragg.calc_loc_datum_time_slots(
		loc,
		sources,
		date_trunc('hour', end_ts),
		interval '1 hour',
		0,
		interval '1 hour')
	INNER JOIN loctz ON loctz.loc_id = loc_id
	ORDER BY ts_start, source_id
$BODY$;

CREATE OR REPLACE FUNCTION solaragg.calc_running_loc_datum_total(
	IN loc bigint,
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	loc_id bigint,
	source_id text,
	jdata jsonb)
LANGUAGE sql
STABLE
ROWS 10 AS
$BODY$
	WITH loctz AS (
		SELECT lids.loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM (SELECT loc AS loc_id) lids
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = lids.loc_id
	)
	SELECT end_ts, end_ts AT TIME ZONE loctz.tz AS local_date, loc, r.source_id, r.jdata
	FROM solaragg.calc_running_total(
		loc,
		sources,
		end_ts,
		TRUE
	) AS r
	INNER JOIN loctz ON loctz.loc_id = loc;
$BODY$;
