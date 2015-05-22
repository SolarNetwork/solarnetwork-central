/**
 * Find aggregated data for a given node over all time up to an optional end date (else the current date).
 * The purpose of this function is to find as few as possible records of already aggregated data
 * so they can be combined into a single running total aggregate result. Each result row includes a
 * <b>weight</b> column that represents the number of hours the given row spans. This number can be 
 * used to calculate a weighted average for all values over the entire result set.
 * 
 * @param node The ID of the node to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts An optional date to limit the results to. If not provided the current date is used.
 */
CREATE OR REPLACE FUNCTION solaragg.find_running_datum(
	IN node bigint, 
	IN sources text[], 
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(ts_start timestamp with time zone, local_date timestamp without time zone, node_id bigint, source_id text, jdata json, weight integer)
LANGUAGE sql 
STABLE AS
$BODY$
	WITH nodetz AS (
		SELECT n.node_id, COALESCE(l.time_zone, 'UTC') AS tz
		FROM solarnet.sn_node n
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = node
	)
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, d.jdata, CAST(extract(epoch from (local_date + interval '1 month') - local_date) / 3600 AS integer) AS weight
	FROM solaragg.agg_datum_monthly d
	INNER JOIN nodetz ON nodetz.node_id = d.node_id
	WHERE d.ts_start < date_trunc('month', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, d.jdata, 24::integer as weight
	FROM solaragg.agg_datum_daily d
	INNER JOIN nodetz ON nodetz.node_id = d.node_id
	WHERE ts_start < date_trunc('day', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.ts_start >= date_trunc('month', end_ts AT TIME ZONE nodetz.tz) AT TIME ZONE nodetz.tz
		AND d.source_id = ANY(sources)
	UNION ALL
	SELECT d.ts_start, d.local_date, d.node_id, d.source_id, d.jdata, 1::INTEGER as weight
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
	INNER JOIN nodetz ON nodetz.node_id = node_id
	ORDER BY ts_start, source_id
$BODY$;


/**
 * Calculate an aggregated value for the running total of data for a given node and set of sources.
 * This function calls solaragg.find_running_datum() internally and calculates the weighted average of
 * all 'i' JSON property values, the sum of all 'a' JSON property values, and the most recent available
 * value for all 's' JSON property values.
 * 
 * @param node The ID of the node to query for.
 * @param sources An array of source IDs to query for.
 * @param end_ts An optional date to limit the results to. If not provided the current date is used.
 */
CREATE OR REPLACE FUNCTION solaragg.calc_running_total(
	IN node bigint, 
	IN sources text[], 
	IN end_ts timestamp with time zone DEFAULT CURRENT_TIMESTAMP)
RETURNS TABLE(node_id bigint, source_id text, jdata json)
LANGUAGE plv8 
STABLE 
ROWS 10 AS
$BODY$
'use strict';

function handleAccumulatingResult(rec, result) {
	var acc = rec.jdata.a,
		aobj = result.aobj,
		prop;
	if ( !acc ) {
		return;
	}
	
	// accumulating data
	for ( prop in acc ) {
		sn.math.util.addto(prop, acc[prop], aobj);
	}
}

function handleInstantaneousResult(rec, result) {
	var inst = rec.jdata.i,
		iobj = result.iobj,
		prop;
	if ( !inst ) {
		return;
	}
	
	// instant data
	for ( prop in inst ) {
		sn.math.util.addto(prop, inst[prop], iobj, rec.weight); // add weighted value
	}
}

function handleStaticResult(rec, result) {
	var stat = rec.jdata.s,
		sobj = result.sobj,
		prop;
	if ( !stat ) {
		return;
	}
	// static data; simply replace anything there, ending up with "most recent" value in results
	for ( prop in stat ) {
		sobj[prop] = stat[prop];
	}
}

function finishResultObject(result) {
	var prop,
		robj,
		tobj;
	// apply weighted average to results
	for ( prop in result.iobj ) {
		result.iobj[prop] /= result.totalWeight;
	}
	
	// generate output record
	robj = {
		node_id : node,
		source_id : result.source_id,
		jdata : {}
	};
	
	// the following for ( prop in X ) things are to only include data in output if object X has values
	
	for ( prop in result.iobj ) {
		robj.jdata.i = sn.util.merge({}, result.iobj); // call merge() to pick up sn.math.util.fixPrecision;
		break;
	}
	for ( prop in result.aobj ) {
		robj.jdata.a = sn.util.merge({}, result.aobj); // call merge() to pick up sn.math.util.fixPrecision
		break;
	}
	for ( prop in result.sobj ) {
		robj.jdata.s = result.sobj;
		break;
	}

	plv8.return_next(robj);
}

(function() {
	var results = {}, // sourceId -> { source_id : 'A', aobj : {}, iobj : {}, sobj : {} }
		sourceId,
		result,
		rec,
		stmt,
		cur;
	
	stmt = plv8.prepare('SELECT * FROM solaragg.find_running_datum($1, $2, $3)', ['bigint', 'text[]', 'timestamp with time zone']);
	cur = stmt.cursor([node, sources, end_ts]);

	while ( rec = cur.fetch() ) {
		if ( !rec.jdata ) {
			continue;
		}
		sourceId = rec.source_id;
		result = results[sourceId];
		if ( result === undefined ) {
			result = { 
				source_id : sourceId, 
				aobj : {}, 
				iobj : {}, 
				sobj: {},
				totalWeight: 0
			};
			results[sourceId] = result;
		}
		result.totalWeight += rec.weight;
		handleAccumulatingResult(rec, result);
		handleInstantaneousResult(rec, result);
		handleStaticResult(rec, result);
	}
	cur.close();
	stmt.free();

	for ( sourceId in results ) {
		finishResultObject(results[sourceId]);
	}
}());
$BODY$;
