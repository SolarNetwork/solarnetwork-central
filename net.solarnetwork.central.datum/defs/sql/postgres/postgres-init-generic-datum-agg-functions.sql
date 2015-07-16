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
	datum_ts timestamp with time zone;
	neighbor solardatum.da_datum;
BEGIN
	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			datum_ts := NEW.ts;
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', datum_ts), NEW.node_id, NEW.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			
			SELECT * FROM solardatum.da_datum d
			WHERE d.ts < datum_ts 
				AND d.ts > datum_ts - interval '1 hour'
				AND d.node_id = NEW.node_id
				AND d.source_id = NEW.source_id
			ORDER BY d.ts DESC
			LIMIT 1
			INTO neighbor;
		ELSE
			datum_ts := OLD.ts;
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', datum_ts), OLD.node_id, OLD.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			
			SELECT * FROM solardatum.da_datum d
			WHERE d.ts < datum_ts 
				AND d.ts > datum_ts - interval '1 hour'
				AND d.node_id = OLD.node_id
				AND d.source_id = OLD.source_id
			ORDER BY d.ts DESC
			LIMIT 1
			INTO neighbor;
	END CASE;
	IF FOUND AND neighbor.ts < date_trunc('hour', datum_ts) THEN
		-- the previous record for this source falls on the previous hour; we have to mark that hour as stale as well
		BEGIN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
		EXCEPTION WHEN unique_violation THEN
			-- Nothing to do, just continue
		END;
	END IF;
	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			SELECT * FROM solardatum.da_datum d
			WHERE d.ts > datum_ts 
				AND d.ts < datum_ts + interval '1 hour'
				AND d.node_id = NEW.node_id
				AND d.source_id = NEW.source_id
			ORDER BY d.ts ASC
			LIMIT 1
			INTO neighbor;
		ELSE
			SELECT * FROM solardatum.da_datum d
			WHERE d.ts > datum_ts 
				AND d.ts < datum_ts + interval '1 hour'
				AND d.node_id = OLD.node_id
				AND d.source_id = OLD.source_id
			ORDER BY d.ts ASC
			LIMIT 1
			INTO neighbor;
	END CASE;
	IF FOUND AND neighbor.ts > date_trunc('hour', datum_ts) THEN
		-- the next record for this source falls on the next hour; we have to mark that hour as stale as well
		BEGIN
			INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
			VALUES (date_trunc('hour', neighbor.ts), neighbor.node_id, neighbor.source_id, 'h');
		EXCEPTION WHEN unique_violation THEN
			-- Nothing to do, just continue
		END;
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
 * Find rows in the <b>solardatum.da_datum</b> table necessary to calculate an hour or more level of
 * aggregate data for a specific duration of time, node, and set of sources. This function will return
 * all available rows within the specified duration, possibly with some rows <em>before</em> or
 * <em>after</em> the duration to enable calculating the actual aggregate over the duration.
 * Each returned row contains a <b>percent</b> value of 0 - 1 that represents the percentage 
 * of time that row falls within the specified duration.
 * 
 * @param node The ID of the node to search for.
 * @param sources An array of one or more source IDs to search for, any of which may match.
 * @param start_ts The start time of the desired time duration.
 * @param span The interval of the time duration, which starts from <b>start_ts</b>.
 * @param tolerance An interval representing the maximum amount of time before between, and after
 *                  rows with the same source ID are allowed to be considered <em>consecutive</em>
 *                  for the purposes of calculating the overall aggregate of the time duration.
 * @out ts The <b>solardatum.da_datum.ts</b> value.
 * @out source_id The <b>solardatum.da_datum.source_id</b> value.
 * @out tsms The <b>solardatum.da_datum.ts</b> value represented as milliseconds since the Unix epoch.
 * @out percent The percent of time this row falls within the specified time duration, from 0 to 1.
 * @out tdiffms The number of milliseconds between this row and the next earliest consecutive row 
 *              (i.e. with a matching <b>source_id</b>).
 * @out jdata The <b>solardatum.da_datum.jdata</b> value.
 * @returns one or more rows of aggregated data
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_slot(
	IN node bigint, 
	IN sources text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts timestamp with time zone, source_id text, tsms bigint, percent real, tdiffms integer, jdata json) AS
$BODY$
SELECT * FROM (
	SELECT 
		d.ts,
		d.source_id,
		CAST(EXTRACT(EPOCH FROM d.ts) * 1000 AS BIGINT) as tsms,
		CASE 
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win > (start_ts + span)
				THEN -1::real
			WHEN d.ts < start_ts
				THEN 0::real
			WHEN d.ts > (start_ts + span) AND lag(d.ts) over win IS NULL
				THEN 0::real
			WHEN d.ts > (start_ts + span)
				THEN (1.0::real - EXTRACT('epoch' FROM (d.ts - (start_ts + span))) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			WHEN lag(d.ts) over win < start_ts
				THEN (EXTRACT('epoch' FROM (d.ts - start_ts)) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			ELSE 1::real
		END AS percent,
		COALESCE(CAST(EXTRACT(EPOCH FROM d.ts - lag(d.ts) over win) * 1000 AS INTEGER), 0) as tdiff,
		d.jdata as jdata
	FROM solardatum.da_datum d
	WHERE d.node_id = node
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE 
	sub.percent > -1
$BODY$
  LANGUAGE sql STABLE;

/**
 * Find rows in the <b>solardatum.da_datum</b> table necessary to calculate minute-level aggregate
 * data for a specific duration of time, node, and set of sources. This function will return
 * all available rows within the specified duration, possibly with some rows <em>before</em> or
 * <em>after</em> the duration to enable calculating the actual aggregate over the duration.
 * All rows are assigned to <b>slotsecs</b> second time slots, and contain a <b>percent</b> 
 * value of 0 - 1 that represents the percentage of time that row falls within the specified 
 * time slot.
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
 * @out ts The <b>solardatum.da_datum.ts</b> value.
 * @out ts_start The minute-level timestamp.
 * @out source_id The <b>solardatum.da_datum.source_id</b> value.
 * @out tsms The <b>solardatum.da_datum.ts</b> value represented as milliseconds since the Unix epoch.
 * @out percent The percent of time this row falls within the specified time duration, from 0 to 1.
 * @out tdiffms The number of milliseconds between this row and the next earliest consecutive row 
 *              (i.e. with a matching <b>source_id</b>).
 * @out jdata The <b>solardatum.da_datum.jdata</b> value.
 * @returns one or more rows of aggregated data
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_for_minute_time_slots(
	IN node bigint, 
	IN sources text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts timestamp with time zone, ts_start timestamp with time zone, source_id text, tsms bigint, percent real, tdiffms integer, jdata json) AS
$BODY$
SELECT * FROM (
	SELECT 
		d.ts,
		solaragg.minute_time_slot(d.ts, slotsecs) as ts_start,
		d.source_id,
		CAST(EXTRACT(EPOCH FROM solaragg.minute_time_slot(d.ts, slotsecs)) * 1000 AS BIGINT) as tsms,
		CASE 
			WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win > (start_ts + span)
				THEN -1::real
			WHEN d.ts < start_ts
				THEN 0::real
			WHEN d.ts > (start_ts + span) AND lag(d.ts) over win IS NULL
				THEN 0::real
			WHEN solaragg.minute_time_slot(lag(d.ts) over win, slotsecs) < solaragg.minute_time_slot(d.ts, slotsecs)
					AND EXTRACT('epoch' FROM d.ts - lag(d.ts) over win) < slotsecs
				THEN (1::real - EXTRACT('epoch' FROM solaragg.minute_time_slot(d.ts, slotsecs) - lag(d.ts) over win) / EXTRACT('epoch' FROM d.ts - lag(d.ts) over win))::real
			ELSE 1::real
		END AS percent,
		COALESCE(CAST(EXTRACT(EPOCH FROM d.ts - lag(d.ts) over win) * 1000 AS INTEGER), 0) as tdiff,
		d.jdata as jdata
	FROM solardatum.da_datum d
	WHERE d.node_id = node
		AND d.source_id = ANY(sources)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts, d.source_id
) AS sub
WHERE 
	sub.percent > -1
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solaragg.calc_datum_time_slots(
	IN node bigint, 
	IN sources text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(ts_start timestamp with time zone, source_id text, jdata json)  LANGUAGE plv8 AS
$BODY$
'use strict';
var runningAvgDiff,
	runningAvgMax = 5,
	toleranceMs = sn.util.intervalMs(tolerance),
	hourFill = {'watts' : 'wattHours'},
	slotMode = (slotsecs > 0 && slotsecs < 3600),
	ignoreLogMessages = (slotMode === true || sn.util.intervalMs(span) !== 3600000),
	logInsertStmt;

function logMessage(nodeId, sourceId, ts, msg) {
	if ( ignoreLogMessages ) {
		return;
	}
	var msg;
	if ( !logInsertStmt ) {
		logInsertStmt = plv8.prepare('INSERT INTO solaragg.agg_messages (node_id, source_id, ts, msg) VALUES ($1, $2, $3, $4)', 
			['bigint', 'text', 'timestamp with time zone', 'text']);
	}
	var dbMsg = Array.prototype.slice.call(arguments, 3).join(' ');
	logInsertStmt.execute([nodeId, sourceId, ts, dbMsg]);
}

function calculateAccumulatingValue(rec, r, val, prevVal, prop, ms) {
	var avgObj = r.accAvg[prop],
		offsetT = 0,
		diff,
		diffT,
		minutes;
	if ( 
			// disallow negative values for records tagged 'power', e.g. inverters that reset each night their reported accumulated energy
			(val < prevVal * 0.5 && rec.jdata.t && Array.isArray(rec.jdata.t) && rec.jdata.t.indexOf('power') >= 0)
			||
			// the running average is 0, the previous value > 0, and the current val <= 1.5% of previous value (i.e. close to 0);
			// don't treat this as a negative accumulation in this case if diff non trivial;
			(prevVal > 0 && (!avgObj || avgObj.average < 1) && val < (prevVal * 0.015))
			) {
		logMessage(node, r.source_id, new Date(rec.tsms), 'Forcing node prevVal', prevVal, 'to 0, val =', val);
		prevVal = 0;
	}
	diff = (val - prevVal);
	minutes = ms / 60000;
	diffT = (diff / minutes);
	if ( avgObj ) {
		if ( avgObj.average > 0 ) {
			offsetT = (diffT / avgObj.average) 
				* (avgObj.next < runningAvgMax ? Math.pow(avgObj.next / runningAvgMax, 2) : 1)
				* (minutes > 2 ? 4 : Math.pow(minutes, 2));
		} else {
			offsetT = (diffT * (minutes > 5 ? 25 : Math.pow(minutes, 2)));
		}
	}
	if ( offsetT > 100 ) {
		logMessage(node, r.source_id, new Date(rec.tsms), 'Rejecting diff', diff, 'offset(t)', offsetT.toFixed(1), 
			'diff(t)', sn.math.util.fixPrecision(diffT, 100), '; ravg', (avgObj ? sn.math.util.fixPrecision(avgObj.average, 100) : 'N/A'), 
			(avgObj ? JSON.stringify(avgObj.samples.map(function(e) { return sn.math.util.fixPrecision(e, 100); })) : 'N/A'));
		return 0;
	}
	maintainAccumulatingRunningAverageDifference(r.accAvg, prop, diffT)
	return diff;
}

function maintainAccumulatingRunningAverageDifference(accAvg, prop, diff) {
	var i,
		avg = 0,
		avgObj = accAvg[prop],
		val,
		samples;
	if ( avgObj === undefined ) {
		avgObj = { samples : new Array(runningAvgMax), average : diff, next : 1 }; // wanted Float32Array, but not available in plv8
		avgObj.samples[0] = diff;
		for ( i = 1; i < runningAvgMax; i += 1 ) {
			avgObj.samples[i] = 0x7FC00000;
		}
		accAvg[prop] = avgObj;
		avg = diff;
	} else {
		samples = avgObj.samples;
		samples[avgObj.next % runningAvgMax] = diff;
		avgObj.next += 1;
		for ( i = 0; i < runningAvgMax; i += 1 ) {
			val = samples[i];
			if ( val === 0x7FC00000 ) {
				break;
			}
			avg += val;
		}
		avg /= i;
		avgObj.average = avg;
	}
}

function finishResultObject(r, endts) {
	var prop,
		robj,
		ri,
		ra;
	if ( r.tsms < start_ts.getTime() || (slotMode && r.tsms >= endts) ) {
		// not included in output because before time start, or end time >= end time
		return;
	}
	robj = {
		ts_start : new Date(r.tsms),
		source_id : r.source_id,
		jdata : {}
	};
	ri = sn.math.util.calculateAverages(r.iobj, r.iobjCounts);
	ra = r.aobj;
	
	for ( prop in ri ) {
		robj.jdata.i = ri;
		break;
	}
	for ( prop in ra ) {
		robj.jdata.a = sn.util.merge({}, ra); // call merge() to pick up sn.math.util.fixPrecision
		break;
	}

	if ( r.prevRec && r.prevRec.percent > 0 ) {
		// merge last record s obj into results, but not overwriting any existing properties
		if ( r.prevRec.jdata.s ) {
			for ( prop in r.prevRec.jdata.s ) {
				robj.jdata.s = r.prevRec.jdata.s;
				break;
			}
		}
		if ( Array.isArray(r.prevRec.jdata.t) && r.prevRec.jdata.t.length > 0 ) {
			robj.jdata.t = r.prevRec.jdata.t;
		}
	}
	plv8.return_next(robj);
}

function handleAccumulatingResult(rec, result) {
	var acc = rec.jdata.a,
		prevAcc = result.prevAcc,
		aobj = result.aobj,
		prop;
	if ( acc && prevAcc && rec.tdiffms <= toleranceMs ) {
		// accumulating data
		for ( prop in acc ) {
			if ( prevAcc[prop] !== undefined ) {				
				sn.math.util.addto(prop, calculateAccumulatingValue(rec, result, acc[prop], prevAcc[prop], prop, rec.tdiffms), aobj, rec.percent);
			}
		}
	}
}

function handleInstantaneousResult(rec, result, onlyHourFill) {
	var inst = rec.jdata.i,
		prevInst = result.prevInst,
		iobj = result.iobj,
		iobjCounts = result.iobjCounts,
		prop,
		propHour;
	if ( inst && rec.percent > 0 && rec.tdiffms <= toleranceMs ) {
		// instant data
		for ( prop in inst ) {
			if ( onlyHourFill !== true ) {
				// only add instantaneous average values for 100% records; we may have to use percent to hour-fill below
				sn.math.util.addto(prop, inst[prop], iobj, 1, iobjCounts);
			}
			if ( result.prevRec && hourFill[prop] ) {
				// calculate hour value, if not already defined for given property
				propHour = hourFill[prop];
				if ( !(rec.jdata.a && rec.jdata.a[propHour]) && prevInst && prevInst[prop] !== undefined ) {
					sn.math.util.addto(propHour, sn.math.util.calculateAverageOverHours(inst[prop], prevInst[prop], rec.tdiffms), result.aobj, rec.percent);
				}
			}
		}
	}
}

function handleFractionalAccumulatingResult(rec, result) {
	var fracRec = {
		source_id 	: rec.source_id,
		tsms		: result.prevRec.tsms,
		percent		: (1 - rec.percent),
		tdiffms		: rec.tdiffms,
		jdata		: rec.jdata
	};
	handleAccumulatingResult(fracRec, result);
	handleInstantaneousResult(fracRec, result, true);
}

(function() {
	var results = {}, // { ts_start : 123, source_id : 'A', aobj : {}, iobj : {}, iobjCounts : {}, sobj : {} ...}
		sourceId,
		result,
		rec,
		prop,
		stmt,
		cur,
		spanMs = sn.util.intervalMs(span),
		endts = start_ts.getTime() + spanMs;
	
	if ( slotMode ) {
		stmt = plv8.prepare('SELECT source_id, tsms, percent, tdiffms, jdata FROM solaragg.find_datum_for_minute_time_slots($1, $2, $3, $4, $5, $6)', 
				['bigint', 'text[]', 'timestamp with time zone', 'interval', 'integer', 'interval']);
		cur = stmt.cursor([node, sources, start_ts, span, slotsecs, tolerance]);
	} else {
		stmt = plv8.prepare('SELECT source_id, tsms, percent, tdiffms, jdata FROM solaragg.find_datum_for_time_slot($1, $2, $3, $4, $5)', 
				['bigint', 'text[]', 'timestamp with time zone', 'interval', 'interval']);
		cur = stmt.cursor([node, sources, start_ts, span, tolerance]);
	}

	while ( rec = cur.fetch() ) {
		if ( !rec.jdata ) {
			continue;
		}
		sourceId = rec.source_id;
		result = results[sourceId];
		if ( result === undefined ) {
			result = { 
				tsms : (slotMode ? rec.tsms : start_ts.getTime()), 
				source_id : sourceId, 
				aobj : {}, 
				iobj : {}, 
				iobjCounts : {}, 
				sobj: {}, 
				accAvg : {}
			};
			results[sourceId] = result;
		} else if ( slotMode && rec.tsms !== result.tsms ) {
			if ( rec.percent < 1 && result.prevRec && result.prevRec.tsms >= start_ts.getTime() ) {
				// add 1-rec.percent to the previous time slot results
				handleFractionalAccumulatingResult(rec, result);
			}
			finishResultObject(result, endts);
			result.tsms = rec.tsms;
			result.aobj = {};
			result.iobj = {};
			result.iobjCounts = {};
			result.sobj = {};
		}
	
		handleAccumulatingResult(rec, result);
		handleInstantaneousResult(rec, result);
	
		result.prevRec = rec;
		result.prevAcc = rec.jdata.a;
		result.prevInst = rec.jdata.i;
	}
	cur.close();
	stmt.free();

	for ( prop in results ) {
		finishResultObject(results[prop], endts);
	}
	
	if ( logInsertStmt ) {
		logInsertStmt.free();
	}
}());
$BODY$ STABLE;

/**
 * Dynamically calculate minute-level time slot aggregate values for a node and set of source IDs.
 * 
 * @param node				node ID
 * @param source			array of source IDs
 * @param start_ts			the start timestamp
 * @param end_ts			the end timestamp
 * @param slotsecs			the number of seconds per time slot, e.g. 600 == 10 minutes.
 */
CREATE OR REPLACE FUNCTION solaragg.find_agg_datum_minute(
	IN node bigint, 
	IN source text[], 
	IN start_ts timestamp with time zone, 
	IN end_ts timestamp with time zone, 
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT interval '1 hour')
  RETURNS TABLE(
	node_id solarcommon.node_id, 
	ts_start timestamp with time zone, 
	local_date timestamp without time zone, 
	source_id solarcommon.source_id,
	jdata json)
  LANGUAGE sql 
  STABLE AS
$BODY$
SELECT 
	n.node_id::solarcommon.node_Id,
	d.ts_start,
	d.ts_start AT TIME ZONE COALESCE(l.time_zone, 'UTC') AS local_date, 
	d.source_id::solarcommon.source_id,
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
	node_id solarcommon.node_id, 
	ts_start timestamp with time zone, 
	local_date timestamp without time zone, 
	source_id solarcommon.source_id,
	jdata json)
  LANGUAGE sql 
  STABLE AS
$BODY$
SELECT
	node::solarcommon.node_id,
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
	node_id solarcommon.node_id, 
	ts_start timestamp with time zone, 
	local_date timestamp without time zone, 
	source_id solarcommon.source_id,
	jdata json)
  LANGUAGE sql 
  STABLE AS
$BODY$
SELECT
	node::solarcommon.node_id,
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
	node_id solarcommon.node_id, 
	ts_start timestamp with time zone, 
	local_date timestamp without time zone, 
	source_id solarcommon.source_id,
	jdata json)
  LANGUAGE sql 
  STABLE AS
$BODY$
SELECT
	node::solarcommon.node_id,
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
	node_id solarcommon.node_id, 
	ts_start timestamp with time zone, 
	local_date timestamp without time zone, 
	source_id solarcommon.source_id,
	jdata json)
  LANGUAGE sql 
  STABLE AS
$BODY$
SELECT
	node::solarcommon.node_id,
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


CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS integer AS
$BODY$
DECLARE
	stale record;
	curs CURSOR FOR SELECT * FROM solaragg.agg_stale_datum 
			WHERE agg_kind = kind
			--ORDER BY ts_start ASC, created ASC, node_id ASC, source_id ASC
			LIMIT 1
			FOR UPDATE;
	agg_span interval;
	agg_json json := NULL;
	node_tz text := 'UTC';
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
		-- get the node TZ for local date/time
		SELECT l.time_zone  FROM solarnet.sn_node n
		INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE n.node_id = stale.node_id
		INTO node_tz;

		IF NOT FOUND THEN
			RAISE NOTICE 'Node % has no time zone, will use UTC.', stale.node_id;
			node_tz := 'UTC';
		END IF;

		SELECT jdata FROM solaragg.calc_datum_time_slots(stale.node_id, ARRAY[stale.source_id::text], 
			stale.ts_start, agg_span, 0, interval '1 hour')
		INTO agg_json;
		IF agg_json IS NULL THEN
			CASE kind
				WHEN 'h' THEN
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start = stale.ts_start;
			END CASE;
		ELSE
			CASE kind
				WHEN 'h' THEN
					<<update_hourly>>
					LOOP
						UPDATE solaragg.agg_datum_hourly SET jdata = agg_json
						WHERE 
							node_id = stale.node_id
							AND source_id = stale.source_id
							AND ts_start = stale.ts_start;

						EXIT update_hourly WHEN FOUND;

						INSERT INTO solaragg.agg_datum_hourly (
							ts_start, local_date, node_id, source_id, jdata)
						VALUES (
							stale.ts_start, 
							stale.ts_start at time zone node_tz,
							stale.node_id,
							stale.source_id,
							agg_json
						);
						EXIT update_hourly;
					END LOOP update_hourly;
				WHEN 'd' THEN
					<<update_daily>>
					LOOP
						UPDATE solaragg.agg_datum_daily SET jdata = agg_json
						WHERE 
							node_id = stale.node_id
							AND source_id = stale.source_id
							AND ts_start = stale.ts_start;

						EXIT update_daily WHEN FOUND;

						INSERT INTO solaragg.agg_datum_daily (
							ts_start, local_date, node_id, source_id, jdata)
						VALUES (
							stale.ts_start, 
							CAST(stale.ts_start at time zone node_tz AS DATE),
							stale.node_id,
							stale.source_id,
							agg_json
						);
						EXIT update_daily;
					END LOOP update_daily;
				ELSE
					<<update_monthly>>
					LOOP
						UPDATE solaragg.agg_datum_monthly SET jdata = agg_json
						WHERE 
							node_id = stale.node_id
							AND source_id = stale.source_id
							AND ts_start = stale.ts_start;

						EXIT update_monthly WHEN FOUND;

						INSERT INTO solaragg.agg_datum_monthly (
							ts_start, local_date, node_id, source_id, jdata)
						VALUES (
							stale.ts_start, 
							CAST(stale.ts_start at time zone node_tz AS DATE),
							stale.node_id,
							stale.source_id,
							agg_json
						);
						EXIT update_monthly;
					END LOOP update_monthly;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;
		result := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		CASE kind
			WHEN 'h' THEN
				BEGIN
					INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
					VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'd');
				EXCEPTION WHEN unique_violation THEN
					-- Nothing to do, just continue
				END;
			WHEN 'd' THEN
				BEGIN
					INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
					VALUES (date_trunc('month', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'm');
				EXCEPTION WHEN unique_violation THEN
					-- Nothing to do, just continue
				END;
			ELSE
				-- nothing
		END CASE;
	END IF;
	CLOSE curs;
	RETURN result;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solaragg.process_agg_stale_datum(kind char, max integer)
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
		SELECT solaragg.process_one_agg_stale_datum(kind) INTO one_result;
		total_result := total_result + one_result;
	END LOOP;
	RETURN total_result;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;
