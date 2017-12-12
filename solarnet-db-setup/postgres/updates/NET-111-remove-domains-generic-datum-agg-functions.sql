DROP FUNCTION solaragg.find_agg_datum_minute(bigint,text[],timestamptz,timestamptz,integer,interval);
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_minute(
	IN node bigint,
	IN source text[],
	IN start_ts timestamp with time zone,
	IN end_ts timestamp with time zone,
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	d.ts_start,
	d.ts_start AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date,
	d.source_id,
	d.jdata
 FROM solaragg.calc_datum_time_slots(
	node,
	source,
	solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs)),
	(end_ts - solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs))),
	solaragg.slot_seconds(slotsecs),
	tolerance
) AS d
JOIN solarnet.sn_node n ON n.node_id = node
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
$BODY$;

DROP FUNCTION solaragg.find_agg_datum_hod(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_hod(
	IN node bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_datum_hourly d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(hour FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_datum_seasonal_hod(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_hod(
	IN node bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_datum_hourly d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(hour FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_datum_dow(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_dow(
	IN node bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2001-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_datum_daily d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_datum_seasonal_dow(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_dow(
	IN node bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2001-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_datum_daily d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_most_recent_hourly(solarcommon.node_id, solarcommon.source_ids);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_hourly(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_hourly AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_hourly d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_hourly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_hourly d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_hourly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

DROP FUNCTION solaragg.find_most_recent_daily(solarcommon.node_id, solarcommon.source_ids);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_daily(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_daily AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_daily d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_daily d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_daily d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_daily d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

DROP FUNCTION solaragg.find_most_recent_monthly(solarcommon.node_id, solarcommon.source_ids);
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_monthly(
	node bigint,
	sources text[] DEFAULT NULL)
  RETURNS SETOF solaragg.agg_datum_monthly AS
$BODY$
BEGIN
	IF sources IS NULL OR array_length(sources, 1) < 1 THEN
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_monthly d
			INNER JOIN (SELECT solardatum.find_available_sources(node) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_monthly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	ELSE
		RETURN QUERY
		WITH maxes AS (
			SELECT max(d.ts_start) as ts_start, d.source_id, node as node_id FROM solaragg.agg_datum_monthly d
			INNER JOIN (SELECT unnest(sources) AS source_id) AS s ON s.source_id = d.source_id
			WHERE d. node_id = node
			GROUP BY d.source_id
		)
		SELECT d.* FROM solaragg.agg_datum_monthly d
		INNER JOIN maxes ON maxes.node_id = d.node_id AND maxes.source_id = d.source_id AND maxes.ts_start = d.ts_start
		ORDER BY d.source_id ASC;
	END IF;
END;$BODY$
  LANGUAGE plpgsql STABLE
  ROWS 20;

DROP FUNCTION solaragg.find_audit_datum_interval(solarcommon.node_id, solarcommon.source_id);
CREATE OR REPLACE FUNCTION solaragg.find_audit_datum_interval(
	IN node bigint,
	IN src text DEFAULT NULL,
	OUT ts_start timestamp with time zone,
	OUT ts_end timestamp with time zone,
	OUT node_tz TEXT,
	OUT node_tz_offset INTEGER)
  RETURNS RECORD AS
$BODY$
BEGIN
	CASE
		WHEN src IS NULL THEN
			SELECT min(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node
			INTO ts_start;
		ELSE
			SELECT min(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node AND source_id = src
			INTO ts_start;
	END CASE;

	CASE
		WHEN src IS NULL THEN
			SELECT max(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node
			INTO ts_end;
		ELSE
			SELECT max(a.ts_start) FROM solaragg.aud_datum_hourly a WHERE node_id = node AND source_id = src
			INTO ts_end;
	END CASE;

	SELECT
		l.time_zone,
		CAST(EXTRACT(epoch FROM z.utc_offset) / 60 AS INTEGER)
	FROM solarnet.sn_node n
	INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	INNER JOIN pg_timezone_names z ON z.name = l.time_zone
	WHERE n.node_id = node
	INTO node_tz, node_tz_offset;

	IF NOT FOUND THEN
		node_tz := 'UTC';
		node_tz_offset := 0;
	END IF;

END;$BODY$
  LANGUAGE plpgsql STABLE;
