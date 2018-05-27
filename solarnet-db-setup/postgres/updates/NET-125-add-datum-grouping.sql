/**
 * Reduce a 2d array into a set of 1d arrays.
 */
CREATE OR REPLACE FUNCTION solarcommon.reduce_dim(anyarray)
  RETURNS SETOF anyarray LANGUAGE plpgsql IMMUTABLE AS
$$
DECLARE
	s $1%TYPE;
BEGIN
	FOREACH s SLICE 1  IN ARRAY $1 LOOP
		RETURN NEXT s;
	END LOOP;
	RETURN;
END;
$$;


-- UPDATE result table to include node_id column, so can be joined to other tables
DROP FUNCTION solaragg.calc_datum_time_slots(bigint, text[], timestamp with time zone, interval, integer, interval);
CREATE OR REPLACE FUNCTION solaragg.calc_datum_time_slots(
	IN node bigint,
	IN sources text[],
	IN start_ts timestamp with time zone,
	IN span interval,
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(node_id bigint, ts_start timestamp with time zone, source_id text, jdata jsonb)
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

-- Fix to work with new node_id column in solaragg.calc_datum_time_slots
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
		SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM solarnet.sn_node n
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = node
		UNION ALL
		SELECT node::bigint AS node_id, 'UTC'::character varying AS tz
		WHERE NOT EXISTS (SELECT node_id FROM solarnet.sn_node WHERE node_id = node)
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

-- At some point in the future, drop this backwards compatible (but no longer used) function
-- DROP FUNCTION solaragg.find_agg_datum_minute(bigint, text[], timestamp with time zone, timestamp with time zone, integer, interval);
