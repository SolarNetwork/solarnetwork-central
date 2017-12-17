DROP FUNCTION solaragg.find_loc_datum_for_time_span(bigint,text[],timestamp with time zone,interval,interval);
CREATE OR REPLACE FUNCTION solaragg.find_loc_datum_for_time_span(
    IN loc bigint,
    IN sources text[],
    IN start_ts timestamp with time zone,
    IN span interval,
    IN tolerance interval DEFAULT '01:00:00'::interval)
  RETURNS TABLE(ts timestamp with time zone, source_id text, jdata jsonb) AS
$BODY$
SELECT sub.ts, sub.source_id, sub.jdata FROM (
	-- subselect filters out "extra" leading/lagging rows from results
	SELECT
		d.ts,
		d.source_id,
		CASE
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win > (start_ts + span)
				THEN TRUE
			ELSE FALSE
		END AS outside,
		solardatum.jdata_from_datum(d) as jdata
	FROM solardatum.da_loc_datum d
	WHERE d.loc_id = loc
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE
	sub.outside = FALSE
$BODY$
  LANGUAGE sql STABLE;

DROP FUNCTION solaragg.calc_loc_datum_time_slots(bigint,text[],timestamp with time zone,interval,integer,interval);
CREATE OR REPLACE FUNCTION solaragg.calc_loc_datum_time_slots(
	IN loc bigint,
	IN sources text[],
	IN start_ts timestamp with time zone,
	IN span interval,
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts_start timestamp with time zone, source_id text, jdata jsonb) LANGUAGE plv8 AS
$BODY$
'use strict';

var intervalMs = require('util/intervalMs').default;
var aggregator = require('datum/aggregator').default;
var slotAggregator = require('datum/slotAggregator').default;

var spanMs = intervalMs(span),
	endTs = start_ts.getTime() + spanMs,
	slotMode = (slotsecs >= 60 && slotsecs <= 1800),
	ignoreLogMessages = (slotMode === true || spanMs !== 3600000),
	stmt,
	cur,
	rec,
	helper,
	aggResult,
	i;

if ( slotMode ) {
	stmt = plv8.prepare(
		'SELECT ts, solaragg.minute_time_slot(ts, '+slotsecs+') as ts_start, source_id, jdata FROM solaragg.find_loc_datum_for_time_span($1, $2, $3, $4, $5)',
		['bigint', 'text[]', 'timestamp with time zone', 'interval', 'interval']);
	helper = slotAggregator({
		startTs : start_ts.getTime(),
		endTs : endTs,
		slotSecs : slotsecs
	});
} else {
	stmt = plv8.prepare(
		'SELECT ts, source_id, jdata FROM solaragg.find_loc_datum_for_time_span($1, $2, $3, $4, $5)',
		['bigint', 'text[]', 'timestamp with time zone', 'interval', 'interval']);
	helper = aggregator({
		startTs : start_ts.getTime(),
		endTs : endTs,
	});
}

cur = stmt.cursor([loc, sources, start_ts, span, tolerance]);

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	aggResult = helper.addDatumRecord(rec);
	if ( aggResult ) {
		plv8.return_next(aggResult);
	}
}
aggResult = helper.finish();
if ( Array.isArray(aggResult) ) {
	for ( i = 0; i < aggResult.length; i += 1 ) {
		plv8.return_next(aggResult[i]);
	}
}

cur.close();
stmt.free();

$BODY$ STABLE;

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
	jdata jsonb)
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
	jdata jsonb)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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
	jdata jsonb)
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
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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
	jdata jsonb)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	loc AS loc_id,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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
	jdata jsonb)
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
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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

CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_loc_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_loc_datum
			WHERE agg_kind = kind
			ORDER BY ts_start ASC, created ASC, loc_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	agg_span interval;
	agg_json jsonb := NULL;
	loc_tz text := 'UTC';
	result integer := 0;
BEGIN
	CASE kind
		WHEN 'h' THEN
			agg_span := interval '1 hour';
		WHEN 'd' THEN
			agg_span := interval '1 day';
		ELSE
			agg_span := interval '1 month';
	END CASE;

	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		-- get the loc TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_loc l
		WHERE l.id = stale.loc_id
		INTO loc_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.loc_id;
			loc_tz := 'UTC';
		END IF;

		SELECT jdata FROM solaragg.calc_loc_datum_time_slots(stale.loc_id, ARRAY[stale.source_id::text],
			stale.ts_start, agg_span, 0, interval '1 hour')
		INTO agg_json;
		IF agg_json IS NULL THEN
			CASE kind
				WHEN 'h' THEN
					DELETE FROM solaragg.agg_loc_datum_hourly
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_loc_datum_daily
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				ELSE
					DELETE FROM solaragg.agg_loc_datum_monthly
					WHERE loc_id = stale.loc_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_loc_datum_hourly (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						stale.ts_start at time zone loc_tz,
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_loc_datum_daily (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;
				ELSE
					INSERT INTO solaragg.agg_loc_datum_monthly (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t')
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_loc_datum WHERE CURRENT OF curs;
		result := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone loc_tz) at time zone loc_tz, stale.loc_id, stale.source_id, 'd')
				ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale.ts_start at time zone loc_tz) at time zone loc_tz, stale.loc_id, stale.source_id, 'm')
				ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
			ELSE
				-- nothing
		END CASE;
	END IF;
	CLOSE curs;
	RETURN result;
END;
$BODY$;

DROP FUNCTION solaragg.find_running_loc_datum(bigint, text[], timestamp with time zone);
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
		SELECT l.id as loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM solarnet.sn_loc l
		WHERE l.id = loc
		UNION ALL
		SELECT loc::bigint AS loc_id, 'UTC'::character varying AS tz
		WHERE NOT EXISTS (SELECT id AS loc_id FROM solarnet.sn_loc WHERE id = loc)
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

DROP FUNCTION solaragg.calc_running_loc_datum_total(bigint, text[],  timestamp with time zone);
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
		SELECT l.id as loc_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM solarnet.sn_loc l
		WHERE l.id = loc
		UNION ALL
		SELECT loc::bigint AS loc_id, 'UTC'::character varying AS tz
		WHERE NOT EXISTS (SELECT id AS loc_id FROM solarnet.sn_loc WHERE id = loc)
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
