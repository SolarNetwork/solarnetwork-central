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

/**
 * Dynamically calculate minute-level time slot aggregate values for nodes and set of source IDs.
 *
 * @param node				array of node IDs
 * @param source			array of source IDs
 * @param start_ts			the start timestamp
 * @param end_ts			the end timestamp
 * @param slotsecs			the number of seconds per time slot, e.g. 600 == 10 minutes.
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
	n.node_id,
	d.ts_start,
	d.ts_start AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date,
	d.source_id,
	d.jdata->'i' AS jdata_i,
	d.jdata->'a' AS jdata_a,
	d.jdata->'s' AS jdata_s,
	solarcommon.json_array_to_text_array(d.jdata->'t') AS jdata_t
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

/* =====
   NEW
   ===== */

CREATE OR REPLACE FUNCTION solaragg.sum_datum_props_grouped_source_minute(
	nodes bigint[],
	sources text[],
	sdate timestamptz,
	edate timestamptz,
	grouped_source text,
	slotsecs integer DEFAULT 600,
	tolerance interval DEFAULT interval '1 hour'
	)
  RETURNS TABLE(ts_start timestamp with time zone, local_date timestamp without time zone, node_id bigint, source_id text, jdata jsonb)
  LANGUAGE sql STABLE AS
$$
	SELECT
		d.ts_start,
		min(d.local_date) AS local_date,
		d.node_id AS node_id,
		grouped_source AS source_id,
		solarcommon.jdata_from_components(
			solarcommon.jsonb_sum_object(d.jdata_i),
			solarcommon.jsonb_sum_object(d.jdata_a),
			NULL,
			NULL)
	FROM solaragg.find_agg_datum_minute(
			nodes,
			sources,
			sdate,
			edate,
			slotsecs,
			tolerance
		) d
	WHERE
		d.node_id = ANY(nodes)
		AND d.source_id = ANY(sources)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	GROUP BY
		d.ts_start, d.node_id;
$$;

CREATE OR REPLACE FUNCTION solaragg.sum_datum_props_grouped_source_hourly(
	nodes bigint[],
	sources text[],
	sdate timestamptz,
	edate timestamptz,
	grouped_source text
	)
  RETURNS TABLE(ts_start timestamp with time zone, local_date timestamp without time zone, node_id bigint, source_id text, jdata jsonb)
  LANGUAGE sql STABLE AS
$$
	SELECT
		d.ts_start,
		min(d.local_date) AS local_date,
		d.node_id AS node_id,
		grouped_source AS source_id,
		solarcommon.jdata_from_components(
			solarcommon.jsonb_sum_object(d.jdata_i),
			solarcommon.jsonb_sum_object(d.jdata_a),
			NULL,
			NULL)
	FROM solaragg.agg_datum_hourly d
	WHERE
		d.node_id = ANY(nodes)
		AND d.source_id = ANY(sources)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	GROUP BY
		d.ts_start, d.node_id;
$$;

CREATE OR REPLACE FUNCTION solaragg.sum_datum_props_grouped_source_daily(
	nodes bigint[],
	sources text[],
	sdate timestamptz,
	edate timestamptz,
	grouped_source text
	)
  RETURNS TABLE(ts_start timestamp with time zone, local_date timestamp without time zone, node_id bigint, source_id text, jdata jsonb)
  LANGUAGE sql STABLE AS
$$
	SELECT
		d.ts_start,
		min(d.local_date) AS local_date,
		d.node_id AS node_id,
		grouped_source AS source_id,
		solarcommon.jdata_from_components(
			solarcommon.jsonb_sum_object(d.jdata_i),
			solarcommon.jsonb_sum_object(d.jdata_a),
			NULL,
			NULL)
	FROM solaragg.agg_datum_daily d
	WHERE
		d.node_id = ANY(nodes)
		AND d.source_id = ANY(sources)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	GROUP BY
		d.ts_start, d.node_id;
$$;

CREATE OR REPLACE FUNCTION solaragg.sum_datum_props_grouped_source_monthly(
	nodes bigint[],
	sources text[],
	sdate timestamptz,
	edate timestamptz,
	grouped_source text
	)
  RETURNS TABLE(ts_start timestamp with time zone, local_date timestamp without time zone, node_id bigint, source_id text, jdata jsonb)
  LANGUAGE sql STABLE AS
$$
	SELECT
		d.ts_start,
		min(d.local_date) AS local_date,
		d.node_id AS node_id,
		grouped_source AS source_id,
		solarcommon.jdata_from_components(
			solarcommon.jsonb_sum_object(d.jdata_i),
			solarcommon.jsonb_sum_object(d.jdata_a),
			NULL,
			NULL)
	FROM solaragg.agg_datum_monthly d
	WHERE
		d.node_id = ANY(nodes)
		AND d.source_id = ANY(sources)
		AND d.ts_start >= sdate
		AND d.ts_start < edate
	GROUP BY
		d.ts_start, d.node_id;
$$;
