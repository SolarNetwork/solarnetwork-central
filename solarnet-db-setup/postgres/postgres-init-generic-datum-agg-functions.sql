-- see CREATE AGGREGATE solaragg.datum_agg_agg(jsonb) below
CREATE OR REPLACE FUNCTION solaragg.datum_agg_agg_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var helper = require('datum/aggAggregate'),
		aggregator;
	if ( !agg_state && el ) {
		aggregator = helper.default('', 0);
		aggregator.addDatumRecord(el);
		agg_state = aggregator.serialize();
	} else if ( agg_state && el ) {
		aggregator = helper.deserialize(agg_state);
		aggregator.addDatumRecord(el); // mutates agg_state
	}
	return agg_state;
$$;


-- see CREATE AGGREGATE solaragg.datum_agg_agg(jsonb) below
CREATE OR REPLACE FUNCTION solaragg.datum_agg_agg_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var helper = require('datum/aggAggregate');
	return (agg_state ? helper.deserialize(agg_state).finish() : {});
$$;


/**
 * Aggregate aggregated datum records into a higher aggregation level.
 *
 * This aggregate accepts aggregate datum objects, such as those from the `agg_datum_monthly` 
 * table, and combines them into an aggregat datum object, such as for a year. The input
 * object must include`jdata` and `jmeta` properties. For example:
 *
 * ```
 * SELECT d.node_id, d.source_id, solaragg.datum_agg_agg(jsonb_build_object(
 *		'jdata', solarnet.jdata_from_components(d.jdata_i, d.jdata_a, d.jdata_s, d.jdata_t),
 *		'jmeta', d.jmeta) ORDER BY d.ts_start) AS jobj
 * FROM solaragg.agg_datum_monthly d
 * WHERE ...
 * GROUP BY d.node_id, d.source_id 
`* ````
 *
 * The resulting object contains the same `jdata` and `jmeta` properties with the results.
 */
CREATE AGGREGATE solaragg.datum_agg_agg(jsonb) (
    sfunc = solaragg.datum_agg_agg_sfunc,
    stype = jsonb,
    finalfunc = solaragg.datum_agg_agg_finalfunc
);


/**
 * Return a valid minute-level time slot seconds value for an arbitrary input value.
 * This function is meant to validate and correct inappropriate input for minute level
 * time slot second values, which must be between 60 and 1800 and evenly divide into 1800.
 *
 * @param secs the seconds to validate
 * @returns integer seconds value, possibly different than the input seconds value
 */
CREATE OR REPLACE FUNCTION solaragg.slot_seconds(secs integer default 600)
  RETURNS integer
  LANGUAGE sql
  IMMUTABLE AS
$BODY$
	SELECT
	CASE
		WHEN secs < 60 OR secs > 1800 OR 1800 % secs <> 0 THEN 600
	ELSE
		secs
	END
$BODY$;

/**
 * Return a normalized minute time-slot timestamp for a given timestamp and slot interval.
 * This function returns the appropriate minute-level time aggregate <code>ts_start</code>
 * value for a given timestamp. For example passing <b>600</b> for <code>sec</code> will
 * return a timestamp who is truncated to <code>:00</code>, <code>:10</code>, <code>:20</code>,
 * <code>:30</code>, <code>:40</code>, or <code>:50</code>.
 *
 * @param ts the timestamp to normalize
 * @param sec the slot seconds
 * @returns normalized timestamp
 */
CREATE OR REPLACE FUNCTION solaragg.minute_time_slot(ts timestamp with time zone, sec integer default 600)
  RETURNS timestamp with time zone
  LANGUAGE sql
  IMMUTABLE AS
$BODY$
	SELECT date_trunc('hour', ts) + (
		ceil(extract('epoch' from ts) - extract('epoch' from date_trunc('hour', ts)))
		- ceil(extract('epoch' from ts))::bigint % sec
	) * interval '1 second'
$BODY$;


/**
 * Find hours with datum data in them based on a node, source, and date range; mark them as "stale"
 * for aggregate processing.
 *
 * This function will insert into the `solaragg.agg_stale_datum` table records for all hours
 * of available data matching the given criteria.
 *
 * @param node 		the node ID of the datum that has been changed (inserted, deleted)
 * @param source 	the source ID of the datum that has been changed
 * @param ts_lower	the lower date of the datum that has changed
 * @param ts_upper	the upper date of the datum that has changed
 */
CREATE OR REPLACE FUNCTION solaragg.mark_datum_stale_hour_slots_range(
		node 		BIGINT,
		source 		CHARACTER VARYING(64),
		ts_lower 	TIMESTAMP WITH TIME ZONE,
		ts_upper 	TIMESTAMP WITH TIME ZONE
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
	SELECT ts_start, node, source, 'h'
	FROM solardatum.calculate_stale_datum(node, source, ts_lower)
	UNION
	SELECT ts_start, node, source, 'h'
	FROM solardatum.calculate_stale_datum(node, source, ts_upper)
	ON CONFLICT DO NOTHING
$$;


/**
 * Trigger that inserts a row into the <b>solaragg.agg_stale_datum<b> table based on
 * a change to a <b>solardatum.da_datum</b> type row. The <b>agg_kind</b> column is
 * set to <code>h</code> and the <b>ts_start</b> column to the changed row's <b>ts</b>
 * timestamp, truncated to the <b>hour</b>. The changed row's <b>node_id</b> and
 * <b>source_id</b> columns are copied as-is. The trigger ignores any
 * a <code>unique_violation</code> exception thrown by the <code>INSERT</code>.
 */
CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_datum()
  RETURNS trigger AS
$BODY$
DECLARE
	neighbor solardatum.da_datum;
BEGIN
	IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
		-- curr hour
		INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
		VALUES (date_trunc('hour', NEW.ts), NEW.node_id, NEW.source_id, 'h')
		ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;

		-- prev hour; if the previous record for this source falls on the previous hour, we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts < NEW.ts
			AND d.ts > NEW.ts - interval '1 hour'
			AND d.node_id = NEW.node_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', NEW.ts) THEN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;
		END IF;

		-- next slot; if there is another record in a future hour, we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts > NEW.ts
			AND d.ts < NEW.ts + interval '3 months'
			AND d.node_id = NEW.node_id
			AND d.source_id = NEW.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', NEW.ts) THEN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;
		END IF;
	END IF;

	IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND (OLD.source_id <> NEW.source_id OR OLD.node_id <> NEW.node_id)) THEN
		-- curr hour
		INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
		VALUES (date_trunc('hour', OLD.ts), OLD.node_id, OLD.source_id, 'h')
		ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;

		-- prev hour; if the previous record for this source falls on the previous hour, we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts < OLD.ts
			AND d.ts > OLD.ts - interval '1 hour'
			AND d.node_id = OLD.node_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts DESC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts < date_trunc('hour', OLD.ts) THEN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;
		END IF;

		-- next slot; if there is another record in a future hour, we have to mark that hour as stale as well
		SELECT * FROM solardatum.da_datum d
		WHERE d.ts > OLD.ts
			AND d.ts < OLD.ts + interval '3 months'
			AND d.node_id = OLD.node_id
			AND d.source_id = OLD.source_id
		ORDER BY d.ts ASC
		LIMIT 1
		INTO neighbor;
		IF FOUND AND neighbor.ts > date_trunc('hour', OLD.ts) THEN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h')
			ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING;
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
 * Dynamically calculate time slot aggregate values for a node and set of source IDs.
 * If <code>slotsecs</code> is between 60 and 1800 then the the results will include
 * corresponding minute-level time slots per source ID. Otherwise at most a single
 * row per source ID will be returned.
 *
 * @param node				node ID
 * @param sources			array of source IDs
 * @param start_ts			the start timestamp
 * @param span				the length of time from start_ts to use as the end timestamp
 * @param slotsecs			the number of seconds per time slot, between 60 and 1800, e.g.
 *                          600 == 10 minutes (the default), 0 == disable
 * @param tolerance			the number of milliseconds tolerance before/after time slots to
 *                          look for adjacent rows
 */
CREATE OR REPLACE FUNCTION solaragg.calc_datum_time_slots(
	node bigint,
	sources text[],
	start_ts timestamp with time zone,
	span interval,
	slotsecs integer DEFAULT 600,
	tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(node_id bigint, ts_start timestamp with time zone, source_id text, jdata jsonb, jmeta jsonb)
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
		'SELECT ts, solaragg.minute_time_slot(ts, '+slotsecs+') as ts_start, source_id, jdata FROM solaragg.find_datum_for_time_span($1, $2, $3, $4, $5)',
		['bigint', 'text[]', 'timestamp with time zone', 'interval', 'interval']);
	helper = slotAggregator({
		startTs : start_ts.getTime(),
		endTs : endTs,
		slotSecs : slotsecs
	});
} else {
	stmt = plv8.prepare(
		'SELECT ts, source_id, jdata FROM solaragg.find_datum_for_time_span($1, $2, $3, $4, $5)',
		['bigint', 'text[]', 'timestamp with time zone', 'interval', 'interval']);
	helper = aggregator({
		startTs : start_ts.getTime(),
		endTs : endTs,
	});
}

cur = stmt.cursor([node, sources, start_ts, span, tolerance]);

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	aggResult = helper.addDatumRecord(rec);
	if ( aggResult ) {
		aggResult.node_id = node;
		plv8.return_next(aggResult);
	}
}
aggResult = helper.finish();
if ( Array.isArray(aggResult) ) {
	for ( i = 0; i < aggResult.length; i += 1 ) {
		aggResult[i].node_id = node;
		plv8.return_next(aggResult[i]);
	}
}

cur.close();
stmt.free();
$BODY$;


/**
 * Aggregate lower-level aggregate data into a higher aggregate level.
 *
 * Use this function to aggregate hourly data into daily, daily into monthly, etc.
 * For example if `span` is `1 day` and `kind` is `h` then the hourly aggregate data
 * starting at `start_ts` to `start_ts + interval '1 day'` would be aggregated and
 * returned from this function.
 *
 * @param node          node ID
 * @param sources       array of source IDs
 * @param start_ts      the start timestamp
 * @param end_ts        the ending timestamp (exclusive) of data to aggregate over; generally the desired output aggregate level
 * @param kind          the type of aggregate data to aggregate, generally one level lower than the desired span
 */
CREATE OR REPLACE FUNCTION solaragg.calc_agg_datum_agg(
	node bigint,
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone,
	kind char)
  RETURNS TABLE(node_id bigint, ts_start timestamp with time zone, source_id text, jdata jsonb, jmeta jsonb)
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
	'SELECT d.ts_start, d.source_id, solaragg.jdata_from_datum(d.*) AS jdata, d.jmeta FROM solaragg.agg_datum_'
	+(kind === 'h' ? 'hourly' : kind === 'd' ? 'daily' : 'monthly')
	+' d WHERE node_id = $1 AND source_id = ANY($2) AND ts_start >= $3 AND ts_start < $4',
	['bigint', 'text[]', 'timestamp with time zone', 'timestamp with time zone']);

cur = stmt.cursor([node, sources, start_ts, end_ts]);

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	helper.addDatumRecord(rec);
}
aggResult = helper.finish();
if ( Array.isArray(aggResult) ) {
	for ( i = 0; i < aggResult.length; i += 1 ) {
		aggResult[i].node_id = node;
		plv8.return_next(aggResult[i]);
	}
}

cur.close();
stmt.free();
$BODY$;


/**
 * Dynamically calculate minute-level time slot aggregate values for nodes and set of source IDs.
 *
 * @param node				array of node IDs
 * @param source			array of source IDs
 * @param start_ts			the start timestamp
 * @param end_ts			the end timestamp
 * @param slotsecs			the number of seconds per time slot, e.g. 600 == 10 minutes.
 * @param tolerance         maximum interval between datum to consider when aggregating
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_minute_data(
	IN node bigint[],
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
	jdata jsonb
  ) LANGUAGE sql STABLE AS
$$
SELECT
	n.node_id,
	d.ts_start,
	d.ts_start AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date,
	d.source_id,
	d.jdata
FROM solarnet.sn_node n
INNER JOIN LATERAL solaragg.calc_datum_time_slots(
	n.node_id,
	source,
	solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs)),
	(end_ts - solaragg.minute_time_slot(start_ts, solaragg.slot_seconds(slotsecs))),
	solaragg.slot_seconds(slotsecs),
	tolerance
) d ON d.node_id = n.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE n.node_id = ANY(node)
$$;


/**
 * Dynamically calculate minute-level time slot aggregate values for nodes and set of source IDs.
 *
 * @param node				array of node IDs
 * @param source			array of source IDs
 * @param start_ts			the start timestamp
 * @param end_ts			the end timestamp
 * @param slotsecs			the number of seconds per time slot, e.g. 600 == 10 minutes.
 * @param tolerance         maximum interval between datum to consider when aggregating
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_minute(
	IN node bigint[],
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
	jdata_i jsonb,
	jdata_a jsonb,
	jdata_s jsonb,
	jdata_t text[]
  ) LANGUAGE sql STABLE AS
$$
SELECT
	d.node_id,
	d.ts_start,
	d.local_date,
	d.source_id,
	d.jdata->'i' AS jdata_i,
	d.jdata->'a' AS jdata_a,
	d.jdata->'s' AS jdata_s,
	solarcommon.json_array_to_text_array(d.jdata->'t') AS jdata_t
FROM solaragg.find_agg_datum_minute_data(
	node,
	source,
	start_ts,
	end_ts,
	slotsecs,
	tolerance
) d
$$;


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


/**
 * Calculate hour-of-day aggregate values for a node and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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
	jdata jsonb)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start,
	(CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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


/**
 * Calculate seasonal hour-of-day aggregate values for a node and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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
	jdata jsonb)
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
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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


/**
 * Calculate day-of-week aggregate values for a node and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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
	jdata jsonb)
  LANGUAGE sql
  STABLE AS
$BODY$
SELECT
	node AS node_id,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start,
	(DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date,
	d.source_id,
	('{"' || path[1] || '":{"' || path[2] || '":'
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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


/**
 * Calculate seasonal day-of-week aggregate values for a node and set of source IDs
 * and one specific general data value. Note that the `path` parameter currently only
 * supports an array with exactly two elements.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
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
	jdata jsonb)
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
		|| ROUND(AVG(CAST(jsonb_extract_path_text(solaragg.jdata_from_datum(d), VARIADIC path) AS double precision)) * 1000) / 1000
		|| '}}')::jsonb as jdata
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


/**
 * Calculate hour-of-day aggregate values for a node and set of source IDs.
 *
 * @param node		node ID
 * @param source	array of source IDs
 * @param start_ts	the start timestamp (defaults to SN epoch)
 * @param end_ts	the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_hod(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AT TIME ZONE 'UTC' AS ts_start
	, (CAST('2001-01-01 ' || to_char(EXTRACT(hour FROM d.local_date), '00') || ':00' AS TIMESTAMP)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_hourly d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(hour FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal hour-of-day aggregate values for a node and set of source IDs.
 *
 * @param node		node ID
 * @param source	array of source IDs
 * @param start_ts	the start timestamp (defaults to SN epoch)
 * @param end_ts	the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_hod(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, solarnet.get_season_monday_start(CAST(d.local_date AS DATE))
		+ CAST(EXTRACT(hour FROM d.local_date) || ' hour' AS INTERVAL) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
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
$$;


/**
 * Calculate day-of-week aggregate values for a node and set of source IDs.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param path				the JSON path to the value to extract, e.g. ['i','watts']
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_dow(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (DATE '2001-01-01' + CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
FROM solaragg.agg_datum_daily d
WHERE
	d.node_id = node
	AND d.source_id = ANY(source)
	AND d.ts_start >= start_ts
	AND d.ts_start < end_ts
GROUP BY
	EXTRACT(isodow FROM d.local_date),
	d.source_id
$$;


/**
 * Calculate seasonal day-of-week aggregate values for a node and set of source IDs.
 *
 * @param node				node ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp (defaults to SN epoch)
 * @param end_ts			the end timestamp (defaults to CURRENT_TIMESTAMP)
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_seasonal_dow(
		node bigint,
		source text[],
		start_ts timestamp with time zone DEFAULT '2008-01-01 00:00+0'::timestamptz,
		end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
	RETURNS TABLE (
		node_id bigint,
		source_id text,
		ts_start timestamp with time zone,
		local_date timestamp without time zone,
		jdata jsonb
	) LANGUAGE sql STABLE AS
$$
SELECT
	node AS node_id
	, d.source_id
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AT TIME ZONE 'UTC' AS ts_start
	, (solarnet.get_season_monday_start(d.local_date)
		+ CAST((EXTRACT(isodow FROM d.local_date) - 1) || ' day' AS INTERVAL)) AS local_date
	, solaragg.datum_agg_agg(jsonb_build_object(
		'jdata', solaragg.jdata_from_datum(d), 'jmeta', d.jmeta) ORDER BY d.ts_start)->'jdata' AS jdata
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
$$;


CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS SETOF solaragg.agg_stale_datum LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					record;
	stale_t_start			timestamp;
	stale_t_end 			timestamp;
	stale_ts_prevstart		timestamptz;
	stale_ts_end 			timestamptz;
	agg_span 				interval;
	agg_json 				jsonb := NULL;
	agg_jmeta 				jsonb := NULL;
	agg_reading 			jsonb := NULL;
	agg_reading_ts_start 	timestamptz := NULL;
	agg_reading_ts_end 		timestamptz := NULL;
	node_tz 				text := 'UTC';
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = kind
		-- Too slow to order; not strictly fair but process much faster
		-- ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
		LIMIT 1
		FOR UPDATE SKIP LOCKED;
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
		-- get the node TZ for local date/time
		SELECT l.time_zone FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = stale.node_id
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.node_id;
			node_tz := 'UTC';
		END IF;
		
		-- stash local time start/end so date calculations for day+ correctly handles DST boundaries
		stale_t_start := stale.ts_start AT TIME ZONE node_tz;
		stale_t_end := stale_t_start + agg_span;
		stale_ts_prevstart := (stale_t_start - agg_span) AT TIME ZONE node_tz;
		stale_ts_end := stale_t_end AT TIME ZONE node_tz;

		CASE kind
			WHEN 'h' THEN
				-- Dramatically faster execution via EXECUTE than embedded SQL here; better query plan
				
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_datum_time_slots($1, $2, $3, $4, $5, $6)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour';
				
				EXECUTE 'SELECT jdata, ts_start, ts_end FROM solardatum.calculate_datum_diff_over($1, $2, $3, $4)'
				INTO agg_reading, agg_reading_ts_start, agg_reading_ts_end
				USING stale.node_id, stale.source_id::text, stale.ts_start, stale.ts_start + agg_span;

			WHEN 'd' THEN
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'h';
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;

			ELSE
				EXECUTE 'SELECT jdata, jmeta FROM solaragg.calc_agg_datum_agg($1, $2, $3, $4, $5)'
				INTO agg_json, agg_jmeta
				USING stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale_ts_end, 'd';
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale_ts_end
				GROUP BY node_id, source_id
				INTO agg_reading;
		END CASE;

		IF agg_json IS NULL AND (agg_reading IS NULL 
				OR (agg_reading_ts_start IS NOT NULL AND agg_reading_ts_start = agg_reading_ts_end)
				) THEN
			-- no data in range, so delete agg row
			-- using date range in case time zone of node has changed
			CASE kind
				WHEN 'h' THEN
					-- note NOT using stale_ts_prevstart here because not needed for hourly
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					INSERT INTO solaragg.agg_datum_hourly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						stale_t_start,
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- no delete from node tz change needed for hourly
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_datum_daily (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
				ELSE
					INSERT INTO solaragg.agg_datum_monthly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale_t_start AS DATE),
						stale.node_id,
						stale.source_id,
						agg_json->'i',
						agg_json->'a',
						agg_json->'s',
						solarcommon.json_array_to_text_array(agg_json->'t'),
						agg_jmeta,
						agg_reading->'as',
						agg_reading->'af',
						agg_reading->'a'
					)
					ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
					SET jdata_i = EXCLUDED.jdata_i,
						jdata_a = EXCLUDED.jdata_a,
						jdata_s = EXCLUDED.jdata_s,
						jdata_t = EXCLUDED.jdata_t,
						jmeta = EXCLUDED.jmeta,
						jdata_as = EXCLUDED.jdata_as,
						jdata_af = EXCLUDED.jdata_af,
						jdata_ad = EXCLUDED.jdata_ad;

					-- in case node tz changed, remove stale record(s)
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale_ts_prevstart
						AND ts_start < stale_ts_end
						AND ts_start <> stale.ts_start;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		-- and also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;

			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'r')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('month', stale_t_start) AT TIME ZONE node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;
		END CASE;
		RETURN NEXT stale;
	END IF;
	CLOSE curs;
END;
$$;


CREATE OR REPLACE FUNCTION solaragg.process_agg_stale_datum(kind char, max integer)
  RETURNS INTEGER LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	one_result INTEGER := 1;
	total_result INTEGER := 0;
BEGIN
	LOOP
		IF one_result < 1 OR (max > -1 AND total_result >= max) THEN
			EXIT;
		END IF;
		PERFORM solaragg.process_one_agg_stale_datum(kind);
		IF FOUND THEN
			one_result := 1;
			total_result := total_result + 1;
		ELSE
			one_result := 0;
		END IF;
	END LOOP;
	RETURN total_result;
END;
$$;

/**
 * Process a single row from the `solaragg.aud_datum_daily_stale` table by performing
 * the appropriate calculations and updating the appropriate audit table with the results.
 *
 * Supported `kind` values are:
 *
 *  * `r` for solardatum.da_datum data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `h` for solaragg.agg_datum_hourly data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `d` for solaragg.agg_datum_daily data rolled up to day values into the `solaragg.aud_datum_daily` table
 *  * `m` for solaragg.agg_datum_monthly data rolled up to month values into the `solaragg.aud_datum_monthly` table
 *
 * When `m` values are processed, the `solaragg.populate_audit_acc_datum_daily()` function
 * will be invoked as well to keep the accumulating audit data updated.
 *
 * @param kind the rollup kind to process
 * @returns the number of rows processed (always 1 or 0)
 */
CREATE OR REPLACE FUNCTION solaragg.process_one_aud_datum_daily_stale(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.aud_datum_daily_stale
			WHERE aud_kind = kind
			ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
	result integer := 0;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO stale;

	IF FOUND THEN
		CASE kind
			WHEN 'r' THEN
				-- raw data counts
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_count)
				SELECT
					node_id,
					source_id,
					stale.ts_start,
					count(*) AS datum_count
				FROM solardatum.da_datum
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts >= stale.ts_start
					AND ts < stale.ts_start + interval '1 day'
				GROUP BY node_id, source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					processed_count = CURRENT_TIMESTAMP;

			WHEN 'h' THEN
				-- hour data counts
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_hourly_count)
				SELECT
					node_id,
					source_id,
					stale.ts_start,
					count(*) AS datum_hourly_count
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < stale.ts_start + interval '1 day'
				GROUP BY node_id, source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_hourly_count = EXCLUDED.datum_hourly_count,
					processed_hourly_count = CURRENT_TIMESTAMP;

			WHEN 'd' THEN
				-- day data counts, including sum of hourly audit prop_count, datum_q_count
				INSERT INTO solaragg.aud_datum_daily (node_id, source_id, ts_start, datum_daily_pres, prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_daily_pres
					FROM solaragg.agg_datum_daily d
					WHERE d.node_id = stale.node_id
					AND d.source_id = stale.source_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.node_id,
					aud.source_id,
					stale.ts_start,
					bool_or(d.datum_daily_pres) AS datum_daily_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solaragg.aud_datum_hourly aud
				CROSS JOIN datum d
				WHERE aud.node_id = stale.node_id
					AND aud.source_id = stale.source_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < stale.ts_start + interval '1 day'
				GROUP BY aud.node_id, aud.source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_daily_pres = EXCLUDED.datum_daily_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed_io_count = CURRENT_TIMESTAMP;

			ELSE
				-- month data counts
				INSERT INTO solaragg.aud_datum_monthly (node_id, source_id, ts_start,
					datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
					prop_count, datum_q_count)
				WITH datum AS (
					SELECT count(*)::integer::boolean AS datum_monthly_pres
					FROM solaragg.agg_datum_monthly d
					WHERE d.node_id = stale.node_id
					AND d.source_id = stale.source_id
					AND d.ts_start = stale.ts_start
				)
				SELECT
					aud.node_id,
					aud.source_id,
					stale.ts_start,
					sum(aud.datum_count) AS datum_count,
					sum(aud.datum_hourly_count) AS datum_hourly_count,
					sum(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END) AS datum_daily_count,
					bool_or(d.datum_monthly_pres) AS datum_monthly_pres,
					sum(aud.prop_count) AS prop_count,
					sum(aud.datum_q_count) AS datum_q_count
				FROM solaragg.aud_datum_daily aud
				INNER JOIN solarnet.node_local_time node ON node.node_id = aud.node_id
				CROSS JOIN datum d
				WHERE aud.node_id = stale.node_id
					AND aud.source_id = stale.source_id
					AND aud.ts_start >= stale.ts_start
					AND aud.ts_start < (stale.ts_start AT TIME ZONE node.time_zone + interval '1 month') AT TIME ZONE node.time_zone
				GROUP BY aud.node_id, aud.source_id
				ON CONFLICT (node_id, ts_start, source_id) DO UPDATE
				SET datum_count = EXCLUDED.datum_count,
					datum_hourly_count = EXCLUDED.datum_hourly_count,
					datum_daily_count = EXCLUDED.datum_daily_count,
					datum_monthly_pres = EXCLUDED.datum_monthly_pres,
					prop_count = EXCLUDED.prop_count,
					datum_q_count = EXCLUDED.datum_q_count,
					processed = CURRENT_TIMESTAMP;
		END CASE;

		CASE kind
			WHEN 'm' THEN
				-- in case node tz changed, remove record(s) from other zone
				-- monthly records clean 1 month on either side
				DELETE FROM solaragg.aud_datum_monthly a
				USING solarnet.node_local_time node
				WHERE node.node_id = stale.node_id
					AND a.node_id = stale.node_id
					AND a.source_id = stale.source_id
					AND a.ts_start > (stale.ts_start AT TIME ZONE node.time_zone - interval '1 month') AT TIME ZONE node.time_zone
					AND a.ts_start < (stale.ts_start AT TIME ZONE node.time_zone + interval '1 month') AT TIME ZONE node.time_zone
					AND a.ts_start <> stale.ts_start;

				-- recalculate full accumulated audit counts for today
				PERFORM solaragg.populate_audit_acc_datum_daily(stale.node_id, stale.source_id);
			ELSE
				-- in case node tz changed, remove record(s) from other zone
				-- daily records clean 1 day on either side
				DELETE FROM solaragg.aud_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start > stale.ts_start - interval '1 day'
					AND ts_start < stale.ts_start + interval '1 day'
					AND ts_start <> stale.ts_start;

				-- recalculate monthly audit based on updated daily values
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				SELECT
					date_trunc('month', stale.ts_start AT TIME ZONE node.time_zone) AT TIME ZONE node.time_zone,
					stale.node_id,
					stale.source_id,
					'm'
				FROM solarnet.node_local_time node
				WHERE node.node_id = stale.node_id
				ON CONFLICT DO NOTHING;
		END CASE;

		-- remove processed stale record
		DELETE FROM solaragg.aud_datum_daily_stale WHERE CURRENT OF curs;
		result := 1;
	END IF;
	CLOSE curs;
	RETURN result;
END;
$BODY$;

/**
 * FUNCTION solardatum.find_most_recent_hourly(bigint[], text[])
 * 
 * Find the highest available hourly data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_hourly(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_hourly_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	INNER JOIN solaragg.agg_datum_hourly_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id AND d.ts_start = date_trunc('hour', mr.ts_max)
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;

/**
 * FUNCTION solardatum.find_most_recent_daily(bigint[], text[])
 * 
 * Find the highest available daily data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_daily(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_daily_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = mr.node_id
	INNER JOIN solaragg.agg_datum_daily_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id 
		AND d.ts_start = date_trunc('day', mr.ts_max AT TIME ZONE COALESCE(nlt.time_zone, 'UTC')) AT TIME ZONE COALESCE(nlt.time_zone, 'UTC')
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;

/**
 * FUNCTION solardatum.find_most_recent_monthly(bigint[], text[])
 * 
 * Find the highest available monthly data for all source IDs for the given node IDs. This query
 * relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find, or NULL/empty array for all available sources
 */
CREATE OR REPLACE FUNCTION solaragg.find_most_recent_monthly(nodes bigint[], sources text[])
RETURNS SETOF solaragg.agg_datum_daily_data  LANGUAGE sql STABLE ROWS 100 AS
$$
	SELECT d.* 
	FROM solardatum.da_datum_range mr
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = mr.node_id
	INNER JOIN solaragg.agg_datum_monthly_data d ON d.node_id = mr.node_id AND d.source_id = mr.source_id 
		AND d.ts_start = date_trunc('month', mr.ts_max AT TIME ZONE COALESCE(nlt.time_zone, 'UTC')) AT TIME ZONE COALESCE(nlt.time_zone, 'UTC')
	WHERE mr.node_id = ANY(nodes) AND (COALESCE(array_length(sources, 1), 0) < 1 OR mr.source_id = ANY(sources))
	ORDER BY d.node_id, d.source_id
$$;


/**
 * Find aggregated data for a given node over all time up to an optional end date (else the current date).
 * The purpose of this function is to find as few as possible records of already aggregated data
 * so they can be combined into a single running total aggregate result. Each result row includes a
 * <b>weight</b> column that represents the number of hours the given row spans. This number can be
 * used to calculate a weighted average for all values over the entire result set.
 *
 * @param node    The ID of the node to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts  An optional date to limit the results to. If not provided the current date is used.
 */
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


/**
 * Calculate a running total for either a node or location ID. There will
 * be at most one result row per source ID in the returned data.
 *
 * @param pk       The ID of the node or location to query for.
 * @param sources  An array of source IDs to query for.
 * @param end_ts   An optional date to limit the results to. If not provided the current date is used.
 * @param loc_mode If TRUE then location datum are queried, otherwise node datum. Defaults to FALSE.
 */
CREATE OR REPLACE FUNCTION solaragg.calc_running_total(
	IN pk bigint,
	IN sources text[],
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
	IN loc_mode boolean DEFAULT FALSE)
RETURNS TABLE(source_id text, jdata jsonb)
LANGUAGE plv8
STABLE
ROWS 10 AS
$BODY$
'use strict';

var totalor = require('datum/totalor').default;

var query = (loc_mode === true
		? 'SELECT * FROM solaragg.find_running_loc_datum($1, $2, $3)'
		: 'SELECT * FROM solaragg.find_running_datum($1, $2, $3)'),
	stmt,
	cur,
	rec,
	helper = totalor(),
	aggResult,
	i;

stmt = plv8.prepare(query, ['bigint', 'text[]', 'timestamp with time zone']);
cur = stmt.cursor([pk, sources, end_ts]);

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	helper.addDatumRecord(rec);
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
 * Calculate a running average of datum up to a specific end date. There will
 * be at most one result row per source ID in the returned data.
 *
 * @param node    The ID of the node to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts  An optional date to limit the results to. If not provided the current date is used.
 */
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
 * Find the minimum/maximum available dates for audit datum.
 *
 * The returned parameters include <code>ts_start</code> and <code>ts_end</code> values
 * for the date range. Additionally the <code>node_tz</code> and <code>node_tz_offset</code>
 * values will provide the time zone of the node, which defaults to UTC if not available.
 *
 * @param node The ID of the node to query for.
 * @param src  An optional source ID to query for. Pass <code>NULL</code> for all sources.
 */
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


/**
 * FUNCTION solaragg.find_available_sources(bigint[])
 * 
 * Find distinct sources for a set of node IDs.
 * This query relies on the `solardatum.da_datum_range` table.
 *
 * @param nodes The IDs of the nodes to query for.
 */
CREATE OR REPLACE FUNCTION solaragg.find_available_sources(nodes bigint[])
RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT node_id, source_id
	FROM solardatum.da_datum_range
	WHERE node_id = ANY(nodes)
	ORDER BY source_id, node_id
$$;


/**
 * Find distinct sources for a set of node IDs and minimum date.
 *
 * This function searches the <code>solaragg.agg_datum_daily</code> table only,
 * so the date filter is approximate.
 *
 * @param nodes The IDs of the nodes to query for.
 * @param sdate The minimum datum date to consider, inclusive.
 */
CREATE OR REPLACE FUNCTION solaragg.find_available_sources_since(nodes bigint[], sdate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
	ORDER BY source_id, node_id
$$;


/**
 * Find distinct sources for a set of node IDs and maximum date.
 *
 * This function searches the <code>solaragg.agg_datum_daily</code> table only,
 * so the date filter is approximate.
 *
 * @param nodes The IDs of the nodes to query for.
 * @param edate The maximum datum date to consider, exclusive.
 */
CREATE OR REPLACE FUNCTION solaragg.find_available_sources_before(nodes bigint[], edate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start < edate
	ORDER BY source_id, node_id
$$;


/**
 * Find distinct sources for a set of node IDs and a date range.
 *
 * This function searches the <code>solaragg.agg_datum_daily</code> table only,
 * so the date filter is approximate.
 *
 * @param nodes The IDs of the nodes to query for.
 * @param sdate The minimum datum date to consider, inclusive.
 * @param edate The maximum datum date to consider, exclusive.
 */
CREATE OR REPLACE FUNCTION solaragg.find_available_sources(
		nodes bigint[],
		sdate timestamp with time zone,
		edate timestamp with time zone)
	RETURNS TABLE(node_id bigint, source_id text) LANGUAGE sql STABLE ROWS 50 AS
$$
	SELECT DISTINCT d.node_id, CAST(d.source_id AS text)
	FROM solaragg.agg_datum_daily d
	WHERE d.node_id = ANY(nodes)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	ORDER BY source_id, node_id
$$;


/**
 * Find hours with datum data in them based on a search criteria.
 *
 * This function can be used to find hour time slots where aggregate
 * data can be computed from.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 * @param start_ts the minimum date (inclusive)
 * @param end_ts the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_hour_slots(
	nodes bigint[],
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	source_id text
  ) LANGUAGE SQL STABLE AS
$$
	SELECT DISTINCT node_id, date_trunc('hour', ts) AS ts_start, source_id
	FROM solardatum.da_datum
	WHERE node_id = ANY(nodes)
		AND source_id = ANY(sources)
		AND ts >= start_ts
		AND ts < end_ts
$$;


/**
 * Find hours with datum data in them based on a search criteria and mark them as "stale" for
 * aggregate processing.
 *
 * This function will insert into the `solaragg.agg_stale_datum` table records for all hours
 * of available data matching the given search criteria.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 * @param start_ts the minimum date (inclusive)
 * @param end_ts the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solaragg.mark_datum_stale_hour_slots(
	nodes bigint[],
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone)
  RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.agg_stale_datum
	SELECT dates.ts_start, dates.node_id, dates.source_id, 'h'
	FROM solaragg.find_datum_hour_slots(nodes, sources, start_ts, end_ts) dates
	ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING
$$;


/**
 * Trigger function to handle a changed (inserted, updated) aggregate datum row.
 *
 * The trigger must be passed the aggregate type as the first trigger argument. It then inserts
 * a row into the `solaragg.agg_stale_flux` for clients to pull from. 
 */
CREATE OR REPLACE FUNCTION solaragg.handle_curr_change()
  RETURNS trigger LANGUAGE 'plpgsql' AS
$$
BEGIN
	INSERT INTO solaragg.agg_stale_flux (agg_kind, node_id, source_id)
	VALUES (TG_ARGV[0], NEW.node_id, NEW.source_id)
	ON CONFLICT (agg_kind, node_id, source_id) DO NOTHING;
	RETURN NULL;
END;
$$;


/**
 * Look for node sources that have no corresponding row in the `solaragg.aud_acc_datum_daily` table
 * on a particular date. The purpose of this is to support populating the accumulating storage
 * date for nodes even if they are offline and not posting data currently.
 *
 * @param ts the date to look for; defaults to the current date
 */
CREATE OR REPLACE FUNCTION solaragg.find_audit_datum_daily_missing(ts date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		node_id bigint,
		source_id character varying(64),
		ts_start timestamp with time zone,
		time_zone character varying(64)
	)
	LANGUAGE sql STABLE AS
$$
	WITH missing AS (
		SELECT r.node_id, r.source_id
		FROM solardatum.da_datum_range r
		EXCEPT
		SELECT a.node_id, a.source_id
		FROM solaragg.aud_acc_datum_daily a
		INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = a.node_id
		WHERE 
			ts_start >= ts::timestamptz - interval '24 hours'
			AND ts_start < ts::timestamptz + interval '24 hours'
			AND ts_start = (((ts AT TIME ZONE nlt.time_zone)::date)::timestamp) AT TIME ZONE nlt.time_zone
	)
	SELECT m.node_id, m.source_id, ts::timestamp AT TIME ZONE nlt.time_zone, nlt.time_zone
	FROM missing m
	INNER JOIN solarnet.node_local_time nlt ON nlt.node_id = m.node_id
$$;


/**
 * Call the `solaragg.find_audit_datum_daily_missing(date)` function and insert the results
 * into the `solaragg.aud_datum_daily_stale` table with an `aud_kind = 'm'` so a record of the found
 * node sources gets generated.
 *
 * The `aud_kind = m` value is used because the processor that handles that record also populates
 * the `solaragg.aud_acc_datum_daily` table.
 *
 * @param ts the date to look for; defaults to the current date
 * @return the number of rows inserted
 */
CREATE OR REPLACE FUNCTION solaragg.populate_audit_datum_daily_missing(ts date DEFAULT CURRENT_DATE)
	RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	ins_count bigint := 0;
BEGIN
	INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
	SELECT 
		date_trunc('month', ts_start at time zone time_zone) at time zone time_zone
		, node_id
		, source_id
		, 'm' AS aud_kind
	FROM solaragg.find_audit_datum_daily_missing(ts)
	ON CONFLICT DO NOTHING;

	GET DIAGNOSTICS ins_count = ROW_COUNT;
	RETURN ins_count;
END;
$$;
