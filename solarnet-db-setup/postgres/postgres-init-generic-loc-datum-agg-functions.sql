/**
 * Trigger that inserts a row into the <b>solaragg.agg_stale_loc_datum<b> table based on
 * a change to a <b>solardatum.da_loc_datum</b> type row. The <b>agg_kind</b> column is
 * set to <code>h</code> and the <b>ts_start</b> column to the changed row's <b>ts</b>
 * timestamp, truncated to the <b>hour</b>. The changed row's <b>loc_id</b> and
 * <b>source_id</b> columns are copied as-is. The trigger ignores any
 * a <code>unique_violation</code> exception thrown by the <code>INSERT</code>.
 */
CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_loc_datum()
  RETURNS trigger AS
$BODY$
DECLARE
	neighbor solardatum.da_loc_datum;
BEGIN
	IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
		-- curr hour
		INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
		VALUES (date_trunc('hour', NEW.ts), NEW.loc_id, NEW.source_id, 'h')
		ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;

		-- prev hour; if the previous record for this source falls on the previous hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_loc_datum d
		WHERE d.ts < NEW.ts
			AND d.ts > NEW.ts - interval '1 hour'
			AND d.loc_id = NEW.loc_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', NEW.ts) THEN
			INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.loc_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
		END IF;

		-- next hour; if the next record for this source falls on the next hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_loc_datum d
		WHERE d.ts > NEW.ts
			AND d.ts < NEW.ts + interval '1 hour'
			AND d.loc_id = NEW.loc_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', NEW.ts) THEN
			INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.loc_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
		END IF;
	END IF;

	IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND (OLD.source_id <> NEW.source_id OR OLD.loc_id <> NEW.loc_id)) THEN
		-- curr hour
		INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
		VALUES (date_trunc('hour', OLD.ts), OLD.loc_id, OLD.source_id, 'h')
		ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;

		-- prev hour; if the previous record for this source falls on the previous hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_loc_datum d
		WHERE d.ts < OLD.ts
			AND d.ts > OLD.ts - interval '1 hour'
			AND d.loc_id = OLD.loc_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', OLD.ts) THEN
			INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.loc_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
		END IF;

		-- next hour; if the next record for this source falls on the next hour; we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_loc_datum d
		WHERE d.ts > OLD.ts
			AND d.ts < OLD.ts + interval '1 hour'
			AND d.loc_id = OLD.loc_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', OLD.ts) THEN
			INSERT INTO solaragg.agg_stale_loc_datum (ts_start, loc_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.loc_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, loc_id, ts_start, source_id) DO NOTHING;
		END IF;
	END IF;

	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			RETURN NEW;
		ELSE
			RETURN OLD;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;


/**
 * Find matching location datum rows for aggregation purposes across a specific time
 * span. The query can return adjacent rows before or after the given span so that
 * accumulating values can be calculated from the results.
 *
 * @param loc				location ID
 * @param sources			array of source IDs
 * @param start_ts			the start timestamp
 * @param span				the length of time from start_ts to use as the end timestamp
 * @param tolerance			the number of milliseconds tolerance before/after span to
 *                          look for adjacent rows
 */
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


/**
 * Dynamically calculate time slot aggregate values for a location and set of source IDs.
 * If <code>slotsecs</code> is between 60 and 1800 then the the results will include
 * corresponding minute-level time slots per source ID. Otherwise at most a single
 * row per source ID will be returned.
 *
 * @param loc				location ID
 * @param sources			array of source IDs
 * @param start_ts			the start timestamp
 * @param span				the length of time from start_ts to use as the end timestamp
 * @param slotsecs			the number of seconds per time slot, between 60 and 1800, e.g.
 *                          600 == 10 minutes (the default), 0 == disable
 * @param tolerance			the number of milliseconds tolerance before/after time slots to
 *                          look for adjacent rows
 */
CREATE OR REPLACE FUNCTION solaragg.calc_loc_datum_time_slots(
	loc bigint,
	sources text[],
	start_ts timestamp with time zone,
	span interval,
	slotsecs integer DEFAULT 600,
	tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts_start timestamp with time zone, source_id text, jdata jsonb, jmeta jsonb)
  LANGUAGE plv8 STABLE AS
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
$BODY$;


/**
 * Aggregate lower-level aggregate location data into a higher aggregate level.
 *
 * Use this function to aggregate hourly data into daily, daily into monthly, etc.
 * For example if `span` is `1 day` and `kind` is `h` then the hourly aggregate data
 * starting at `start_ts` to `start_ts + interval '1 day'` would be aggregated and
 * returned from this function.
 *
 * @param loc          the location ID
 * @param sources      array of source IDs
 * @param start_ts     the start timestamp
 * @param end_ts       the ending timestamp (exclusive) of data to aggregate over; generally the desired output aggregate level
 * @param kind         the type of aggregate data to aggregate, generally one level lower than the desired span
 */
CREATE OR REPLACE FUNCTION solaragg.calc_agg_loc_datum_agg(
	loc bigint,
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone,
	kind char)
  RETURNS TABLE(loc_id bigint, ts_start timestamp with time zone, source_id text, jdata jsonb, jmeta jsonb)
  LANGUAGE plv8 STABLE AS
$BODY$
'use strict';

var aggregator = require('datum/aggregator').default;

var stmt,
	cur,
	rec,
	helper,
	aggResult,
	i;

helper = aggregator({
	startTs : start_ts.getTime(),
	endTs : end_ts.getTime(),
});

stmt = plv8.prepare(
	'SELECT d.ts_start, d.source_id, solaragg.jdata_from_datum(d.*) AS jdata, d.jmeta FROM solaragg.agg_loc_datum_'
	+(kind === 'h' ? 'hourly' : kind === 'd' ? 'daily' : 'monthly')
	+' d WHERE loc_id = $1 AND source_id = ANY($2) AND ts_start >= $3 AND ts_start < $4',
	['bigint', 'text[]', 'timestamp with time zone', 'timestamp with time zone']);

cur = stmt.cursor([loc, sources, start_ts, end_ts]);

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	helper.addDatumRecord(rec);
}
aggResult = helper.finish();
if ( Array.isArray(aggResult) ) {
	for ( i = 0; i < aggResult.length; i += 1 ) {
		aggResult[i].loc_id = loc;
		plv8.return_next(aggResult[i]);
	}
}

cur.close();
stmt.free();
$BODY$;


/**
 * Dynamically calculate minute-level time slot aggregate values for a loc and set of source IDs.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp
 * @param end_ts			the end timestamp
 * @param slotsecs			the number of seconds per time slot, e.g. 600 == 10 minutes.
 */
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


/**
 * Calculate hour-of-day aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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


/**
 * Calculate seasonal hour-of-day aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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


/**
 * Calculate day-of-week aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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


/**
 * Calculate seasonal day-of-week aggregate values for a loc and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param loc				loc ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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
			-- Too slow to order; not strictly fair but process much faster
			-- ORDER BY ts_start ASC, created ASC, loc_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	agg_span interval;
	agg_json jsonb := NULL;
	agg_jmeta jsonb := NULL;
	loc_tz text := 'UTC';
	proc_count integer := 0;
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
			RAISE NOTICE 'Location % has no time zone, will use UTC.', stale.loc_id;
			loc_tz := 'UTC';
		END IF;

		CASE kind
			WHEN 'h' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_loc_datum_time_slots(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour')
				INTO agg_json, agg_jmeta;

			WHEN 'd' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_loc_datum_agg(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start, stale.ts_start + agg_span, 'h')
				INTO agg_json, agg_jmeta;

			ELSE
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_loc_datum_agg(stale.loc_id, ARRAY[stale.source_id::text], stale.ts_start, stale.ts_start + agg_span, 'd')
				INTO agg_json, agg_jmeta;
		END CASE;

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
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						stale.ts_start at time zone loc_tz,
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;

				WHEN 'd' THEN
					INSERT INTO solaragg.agg_loc_datum_daily (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;
				ELSE
					INSERT INTO solaragg.agg_loc_datum_monthly (
						ts_start, local_date, loc_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone loc_tz AS DATE),
						stale.loc_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta
					)
					ON CONFLICT (loc_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_loc_datum WHERE CURRENT OF curs;
		proc_count := 1;

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
	RETURN proc_count;
END;
$BODY$;

CREATE OR REPLACE FUNCTION solaragg.process_agg_stale_loc_datum(kind char, max integer)
  RETURNS INTEGER AS
$BODY$
DECLARE
	one_result INTEGER := 1;
	total_result INTEGER := 0;
BEGIN
	LOOP
		IF one_result < 1 OR (max > -1 AND total_result >= max) THEN
			EXIT;
		END IF;
		SELECT solaragg.process_one_agg_stale_loc_datum(kind) INTO one_result;
		total_result := total_result + one_result;
	END LOOP;
	RETURN total_result;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;


/**
 * Find aggregated data for a given location over all time up to an optional end date (else the current date).
 * The purpose of this function is to find as few as possible records of already aggregated data
 * so they can be combined into a single running total aggregate result. Each result row includes a
 * <b>weight</b> column that represents the number of hours the given row spans. This number can be
 * used to calculate a weighted average for all values over the entire result set.
 *
 * @param loc The ID of the location to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts An optional date to limit the results to. If not provided the current date is used.
 */
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


/**
 * Calculate a running average of location datum up to a specific end date. There will
 * be at most one result row per source ID in the returned data.
 *
 * @param loc     The ID of the location to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts  An optional date to limit the results to. If not provided the current date is used.
 */
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
