CREATE OR REPLACE FUNCTION solaragg.minute_time_slot(ts timestamp with time zone, sec integer default 600)
  RETURNS timestamp with time zone AS
$BODY$
	SELECT date_trunc('hour', ts) + (
		ceil(extract('epoch' from ts) - extract('epoch' from date_trunc('hour', ts))) 
		- ceil(extract('epoch' from ts))::bigint % sec
	) * interval '1 second'
$BODY$
  LANGUAGE sql IMMUTABLE;

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

CREATE OR REPLACE FUNCTION solaragg.find_datum_samples_for_time_slot(
	IN node bigint, 
	IN source text, 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN spill interval DEFAULT interval '20 minutes')
  RETURNS TABLE(ts timestamp with time zone, tsms bigint, percent real, tdiffms integer, jdata json) AS
$BODY$
WITH prevd AS (
	/*
	Find the previous datum, but only look up to 'spill' interval prior. This query also handles the situation 
	where there is no data earlier than the start of the specified time span, e.g. at the very start of the data.
	*/
	SELECT d.ts, 
		ABS(EXTRACT('epoch' FROM (d.ts - start_ts))) as dt, 
		CASE WHEN d.ts < start_ts THEN 0 ELSE 1 END as earlier
	FROM solardatum.da_datum d
	WHERE 
		d.node_id = node
		and d.source_id = source
		and d.ts >= (start_ts - spill)
		and d.ts < (start_ts + span)
		ORDER BY earlier ASC, dt ASC
		LIMIT 1),
nextd AS (
	/*
	Find the next datum, but only look up to 'spill' interval after this query also handles the situation
	where there is no data later than the end of the specified time span, e.g. at the very end of the data.
	*/
	SELECT d.ts, 
		ABS(EXTRACT('epoch' FROM (d.ts - (start_ts + span)))) as dt, 
		CASE WHEN d.ts < (start_ts + span) THEN 0 ELSE 1 END as earlier
	FROM solardatum.da_datum d
	WHERE 
		d.node_id = node
		and d.source_id = source
		and d.ts >= start_ts
		and d.ts < (start_ts + span +spill)
		ORDER BY earlier DESC, dt ASC
		LIMIT 1)
SELECT 
	d.ts,
	CAST(EXTRACT(EPOCH FROM d.ts) * 1000 AS BIGINT) as tsms,
		CASE 
			WHEN d.ts < start_ts THEN 0::real
			WHEN d.ts > (start_ts + span) AND lag(d.ts) over win IS NULL THEN 0::real
			WHEN d.ts > (start_ts + span)
				THEN (1.0::real - EXTRACT('epoch' FROM (d.ts - (start_ts + span))) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			WHEN lag(d.ts) over win < start_ts
				THEN (EXTRACT('epoch' FROM (d.ts - start_ts)) / EXTRACT('epoch' FROM (d.ts - lag(d.ts) over win)))::real
			ELSE 1.0
		END AS percent,
	COALESCE(CAST(EXTRACT(EPOCH FROM d.ts - lag(d.ts) over win) * 1000 AS INTEGER), 0) as tdiff,
	d.jdata as jdata
FROM solardatum.da_datum d
WHERE d.node_id = node
	AND d.source_id = source
	AND d.ts >= (select ts FROM prevd)
	AND d.ts <= (select ts FROM nextd)
WINDOW win AS (order by d.ts)
ORDER BY d.ts
$BODY$
  LANGUAGE sql STABLE;

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

CREATE OR REPLACE FUNCTION solaragg.calc_datum_time_slot(
	IN node bigint, 
	IN source text, 
	IN start_ts timestamp with time zone, 
	IN span interval, 
	IN spill interval DEFAULT interval '20 minutes')
  RETURNS json  LANGUAGE plv8 AS
$BODY$
'use strict';
var stmt = plv8.prepare('SELECT tsms, percent, tdiffms, jdata FROM solaragg.find_datum_samples_for_time_slot($1, $2, $3, $4, $5)', 
				['bigint', 'text', 'timestamp with time zone', 'interval', 'interval']),
	cur = stmt.cursor([node, source, start_ts, span, spill]),
	iobj = {},
	iobjCounts = {},
	aobj = {},
	sobj = {},
	robj,
	acc,
	prevAcc,
	accAvg = {},
	inst,
	prevInst,
	rec,
	prevRec,
	obj,
	prop,
	propHour,
	val,
	runningAvgDiff,
	runningAvgMax = 5,
	toleranceMs,
	hourFill = {'watts' : 'wattHours'};

toleranceMs = (function() {
	// calculate the number of milliseconds in the spill interval, which plv8 gives us as a string which we look for HH:MM:SS format
	var hms = spill.match(/(\d{2}):(\d{2}):(\d{2})$/);
	if ( hms && hms.length === 4 ) {
		return ((hms[1] * 60 * 60 * 1000) + (hms[2] * 60 * 1000) + (hms[3] * 1000));
	}
	return 0;
}());

function calculateAccumulatingValue(val, prevVal, prop, ms) {
	var diff = (val - prevVal),
		diffT = Math.abs(diff / (ms / 60000)),
		offsetT = 0,
		avgObj = accAvg[prop];
	if ( avgObj && avgObj.average > 0 ) {
		offsetT = (diffT / avgObj.average) * Math.pow(avgObj.samples.length / runningAvgMax, 2) 
			* (ms > 60000 ? 1 : Math.pow(ms/60000, 2));
	}
	if ( offsetT > 100 ) {
		plv8.elog(NOTICE, 'Rejecting node', node, 'source', source, '@', new Date(rec.tsms), 'diff', diff, 'offset', offsetT.toFixed(1), '>100');
		plv8.elog(NOTICE, 'Diff', diffT.toFixed(2), '; running average', avgObj.average, JSON.stringify(avgObj.samples));
		return 0;
	}
	maintainAccumulatingRunningAverageDifference(prop, diffT)
	return diff;
}

function maintainAccumulatingRunningAverageDifference(prop, diff) {
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

while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	acc = rec.jdata.a;
	if ( acc && prevAcc && rec.tdiffms <= toleranceMs ) {
		// accumulating data
		for ( prop in acc ) {
			if ( prevAcc[prop] !== undefined ) {				
				sn.math.util.addto(prop, calculateAccumulatingValue(acc[prop], prevAcc[prop], prop, rec.tdiffms), aobj, rec.percent);
			}
		}
	}
	inst = rec.jdata.i;
	if ( inst ) {
		// instant data
		for ( prop in inst ) {
			if ( rec.tdiffms > toleranceMs ) {
				continue;
			}
			sn.math.util.addto(prop, inst[prop], iobj, rec.percent, iobjCounts);
			if ( prevRec && hourFill[prop] ) {
				// calculate hour value, if not already defined for given property
				propHour = hourFill[prop];
				if ( !(acc && acc[propHour]) && prevInst && prevInst[prop] !== undefined ) {
					sn.math.util.addto(propHour, sn.math.util.calculateAverageOverHours(inst[prop], prevInst[prop], rec.tdiffms), aobj, rec.percent);
				}
			}
		}
	}
	prevRec = rec;
	prevAcc = acc;
	prevInst = inst;
}
cur.close();
stmt.free();

robj = {};

iobj = sn.math.util.calculateAverages(iobj, iobjCounts);
for ( prop in iobj ) {
	robj['i'] = iobj;
	break;
}
for ( prop in aobj ) {
	robj['a'] = sn.util.merge({}, aobj); // call merge() to pick up sn.math.util.fixPrecision
	break;
}

if ( prevRec && prevRec.percent > 0 ) {
	// merge last record s obj into results, but not overwriting any existing properties
	if ( prevRec.jdata.s ) {
		for ( prop in prevRec.jdata.s ) {
			robj['s'] = prevRec.jdata.s;
		}
	}
	if ( Array.isArray(prevRec.jdata.t) && prevRec.jdata.t.length > 0 ) {
		robj['t'] = prevRec.jdata.t;
	}
}

return robj;
$BODY$ STABLE;


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

		SELECT solaragg.calc_datum_time_slot(stale.node_id, stale.source_id, stale.ts_start, agg_span, interval '20 minutes')
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
