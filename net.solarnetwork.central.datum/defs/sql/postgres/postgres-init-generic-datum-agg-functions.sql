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

CREATE OR REPLACE FUNCTION solardatum.trigger_agg_stale_datum()
  RETURNS trigger AS
$BODY$BEGIN
	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', NEW.ts), NEW.node_id, NEW.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN NEW;
		ELSE
			BEGIN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('hour', OLD.ts), OLD.node_id, OLD.source_id, 'h');
			EXCEPTION WHEN unique_violation THEN
				-- Nothing to do, just continue
			END;
			RETURN OLD;
	END CASE;
END;$BODY$
  LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_slot(
	IN node bigint, 
	IN source text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN tolerance interval DEFAULT interval '20 minutes')
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
			WHEN d.ts < start_ts OR lag(d.ts) over win > (start_ts + span)
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
		AND d.source_id = ANY(source)
		AND d.ts >= start_ts - tolerance
		AND d.ts <= start_ts + span + tolerance
	WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	ORDER BY d.ts
) AS sub
WHERE 
	sub.percent > -1
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solaragg.find_datum_for_minute_time_slots(
	IN node bigint, 
	IN source text[], 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN slotsecs integer DEFAULT 600,
	IN tolerance interval DEFAULT '00:20:00'::interval)
  RETURNS TABLE(ts timestamp with time zone, ts_start timestamp with time zone, source_id text, tsms bigint, percent real, tdiffms integer, jdata json) AS
$BODY$
SELECT * FROM (
	SELECT 
		d.ts,
		solaragg.minute_time_slot(d.ts, slotsecs) as ts_start,
		d.source_id,
		CAST(EXTRACT(EPOCH FROM solaragg.minute_time_slot(d.ts, slotsecs)) * 1000 AS BIGINT) as tsms,
		CASE 
			WHEN lead(d.ts) over win < start_ts OR d.ts > (start_ts + span)
				THEN -1::real
			WHEN d.ts < start_ts OR lag(d.ts) over win > (start_ts + span)
				THEN 0::real
			WHEN d.ts > (start_ts + span) AND lag(d.ts) over win IS NULL
				THEN 0::real
			WHEN solaragg.minute_time_slot(lag(d.ts) over win, slotsecs) < solaragg.minute_time_slot(d.ts, slotsecs)
				THEN (1::real - EXTRACT('epoch' FROM solaragg.minute_time_slot(d.ts, slotsecs) - lag(d.ts) over win) / EXTRACT('epoch' FROM d.ts - lag(d.ts) over win))::real
			ELSE 1::real
		END AS percent,
		COALESCE(CAST(EXTRACT(EPOCH FROM d.ts - lag(d.ts) over win) * 1000 AS INTEGER), 0) as tdiff,
		d.jdata as jdata
	FROM solardatum.da_datum d
	WHERE d.node_id = node
		AND d.source_id = ANY(source)
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
	IN tolerance interval DEFAULT interval '20 minutes')
  RETURNS TABLE(ts_start timestamp with time zone, source_id text, jdata json)  LANGUAGE plv8 AS
$BODY$
'use strict';
var runningAvgDiff,
	runningAvgMax = 5,
	toleranceMs,
	hourFill = {'watts' : 'wattHours'};

toleranceMs = (function() {
	// calculate the number of milliseconds in the tolerance interval, which plv8 gives us as a string which we look for HH:MM:SS format
	var hms = tolerance.match(/(\d{2}):(\d{2}):(\d{2})$/);
	if ( hms && hms.length === 4 ) {
		return ((hms[1] * 60 * 60 * 1000) + (hms[2] * 60 * 1000) + (hms[3] * 1000));
	}
	return 0;
}());

function calculateAccumulatingValue(r, val, prevVal, prop, ms) {
	var diff = (val - prevVal),
		diffT = Math.abs(diff / (ms / 60000)),
		offsetT = 0,
		avgObj = r.accAvg[prop];
	if ( avgObj && avgObj.average > 0 ) {
		offsetT = (diffT / avgObj.average) * Math.pow(avgObj.samples.length / runningAvgMax, 2) 
			* (ms > 60000 ? 1 : Math.pow(ms/60000, 2));
	}
	if ( offsetT > 100 ) {
		plv8.elog(NOTICE, 'Rejecting node', node, 'source', source, '@', new Date(rec.tsms), 'diff', diff, 'offset', offsetT.toFixed(1), '>100');
		plv8.elog(NOTICE, 'Diff', diffT.toFixed(2), '; running average', avgObj.average, JSON.stringify(avgObj.samples));
		return 0;
	}
	maintainAccumulatingRunningAverageDifference(r.accAvg, prop, diffT)
	return diff;
}

function maintainAccumulatingRunningAverageDifference(accAvg, prop, diff) {
	var i = 0,
		avg = 0,
		avgObj = accAvg[prop];
	if ( diff === 0 ) {
		return;
	}
	if ( avgObj === undefined ) {
		avgObj = { samples : [diff], average : diff };
		accAvg[prop] = avgObj;
		avg = diff;
	} else {
		if ( avgObj.samples.length >= runningAvgMax ) {
			avgObj.samples.shift();
		}
		avgObj.samples.push(diff);
		while ( i < avgObj.samples.length ) {
			avg += avgObj.samples[i];
			i += 1;
		}
		avg /= avgObj.samples.length;
		avgObj.average = avg;
	}
}

function finishResultObject(r) {
	var prop,
		robj,
		ri,
		ra;
	if ( r.tsms < start_ts.getTime() /*|| r.tsms > start_ts.getTime() +*/  ) {
		// not included in output because before time start
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
				sn.math.util.addto(prop, calculateAccumulatingValue(result, acc[prop], prevAcc[prop], prop, rec.tdiffms), aobj, rec.percent);
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
		slotMode = (slotsecs > 0 && slotsecs < 3600);
	
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
			finishResultObject(result);
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
		finishResultObject(results[prop]);
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
	IN tolerance interval DEFAULT '00:20:00'::interval)
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
			stale.ts_start, agg_span, 0, interval '20 minutes')
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
