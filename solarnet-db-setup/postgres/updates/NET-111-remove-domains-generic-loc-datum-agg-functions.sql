DROP FUNCTION solaragg.find_agg_loc_datum_minute(bigint,text[],timestamptz,timestamptz,integer,interval);
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_minute(
	IN loc bigint,
	IN source text[],
	IN start_ts timestamp with time zone,
	IN end_ts timestamp with time zone,
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(
	loc_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	d.ts_start,
	d.ts_start AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date,
	d.source_id,
	d.jdata
 FROM solaragg.calc_loc_datum_time_slots(
	loc,
	source,
	solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs)),
	(end_ts - solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs))),
	solaragg.slot_seconds(slotsecs),
	tolerance
) AS d
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = loc
$BODY$;

DROP FUNCTION solaragg.find_agg_loc_datum_hod(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_hod(
	IN loc bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	loc_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_loc_datum_hourly d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(hour FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_loc_datum_seasonal_hod(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_seasonal_hod(
	IN loc bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	loc_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_loc_datum_hourly d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(hour FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_loc_datum_dow(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_dow(
	IN loc bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2001-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	loc_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_loc_datum_daily d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$BODY$;

DROP FUNCTION solaragg.find_agg_loc_datum_seasonal_dow(bigint,text[],text[],timestamptz,timestamptz);
CREATE OR REPLACE FUNCTION solaragg.find_agg_loc_datum_seasonal_dow(
	IN loc bigint,
	IN source text[],
	IN path text[],
	IN start_ts timestamp with time zone DEFAULT '2001-01-01 00:00+0'::timestamptz,
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
  RETURNS TABLE(
	loc_id bigint,
	ts_start timestamp with time zone,
	local_date timestamp without time zone,
	source_id text,
	jdata json)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(json_extract_path_text(jdata, VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::json as jdata
FROM solaragg.agg_loc_datum_daily d
WHERE
	d.loc_id = loc
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	solarnet.get_season_monday_start(CAST(d.local_date AS date)),
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$BODY$;
