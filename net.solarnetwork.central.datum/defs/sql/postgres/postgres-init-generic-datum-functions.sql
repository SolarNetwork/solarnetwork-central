CREATE OR REPLACE FUNCTION solaragg.find_datum_samples_for_time_slot(IN node bigint, IN source text, IN start_ts timestamp with time zone, IN span interval, IN spill interval)
  RETURNS TABLE(ts timestamp with time zone, tsms bigint, percent real, tdiffms integer, jdata json) AS
$BODY$
WITH prevd AS (
	/*
	find the previous datum, but only look up to 1 hour prior
	this query also handles the situation where there is no data earlier than the start of the specified time span,
	e.g. at the very start of the data
	*/
	SELECT d.ts, 
		ABS(EXTRACT('epoch' FROM (d.ts - start_ts))) as dt, 
		CASE WHEN d.ts < start_ts THEN 0 ELSE 1 END as earlier
	FROM solardatum.sn_datum d
	WHERE 
		d.node_id = node
		and d.source_id = source
		and d.ts >= (start_ts - spill)
		and d.ts < (start_ts + span)
		ORDER BY earlier ASC, dt ASC
		LIMIT 1),
nextd AS (
	/*
	find the next datum, but only look up to 1 hour after
	this query also handles the situation where there is no data later than the end of the specified time span,
	e.g. at the very end of the data
	*/
	SELECT d.ts, 
		ABS(EXTRACT('epoch' FROM (d.ts - (start_ts + span)))) as dt, 
		CASE WHEN d.ts < (start_ts + span) THEN 0 ELSE 1 END as earlier
	FROM solardatum.sn_datum d
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
FROM solardatum.sn_datum d
WHERE d.node_id = node
	AND d.source_id = source
	AND d.ts >= (select ts FROM prevd)
	AND d.ts <= (select ts FROM nextd)
WINDOW win AS (order by d.ts)
ORDER BY d.ts
$BODY$
  LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION solaragg.calc_datum_time_slot(IN node bigint, IN source text, IN start_ts timestamp with time zone, IN span interval, IN spill interval)
  RETURNS json AS
$BODY$
'use strict';
var stmt = plv8.prepare('SELECT tsms, percent, tdiffms, jdata FROM solaragg.find_datum_samples_for_time_slot($1, $2, $3, $4, $5)', 
				['bigint', 'text', 'timestamp with time zone', 'interval', 'interval']),
	cur = stmt.cursor([node, source, start_ts, span, spill]),
	iobj = {},
	iobjCounts = {},
	aobj = {},
	acc,
	prevAcc,
	inst,
	prevInst,
	rec,
	prevRec,
	obj,
	prop,
	propHour,
	val,
	hourFill = {'watts' : 'watt_hours'};

function addto(k, v, o, p, c) {
	if ( o[k] === undefined ) {
		o[k] = (v * p);
	} else {
		o[k] += (v * p);
	}
	if ( c ) {
		if ( c[k] === undefined ) {
			c[k] = 1;
		} else {
			c[k] += 1;
		}
	}
}

function calculateAverageOverHours(w, prevW, milli) {
	return (Math.abs((w + prevW) / 2) * (milli / 3600000));
}

function fixPrecision(val) {
	if ( typeof val !== 'number' ) {
		return val;
	}
	return (Math.round(val* 1000) / 1000);
}

function calculateAverages(obj, counts) {
	var prop, count, result = {};
	if ( !obj ) {
		return result;
	}
	if ( !counts ) {
		return obj;
	}
	for ( prop in obj ) {
		if ( obj.hasOwnProperty(prop) ) {
			count = counts[prop];
			if ( count > 0 ) {
				result[prop] = fixPrecision(obj[prop] / count);
			}
		}
	}
	return result;
}

function merge(result, obj) {
	var prop;
	if ( obj ) {	
		for ( prop in obj ) {
			if ( obj.hasOwnProperty(prop) ) {
				result[prop] = fixPrecision(obj[prop]);
			}
		}
	}

	return result;
}
	
while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	acc = rec.jdata.a;
	if ( prevRec && acc ) {
		// accumulating data
		for ( prop in acc ) {
			if ( acc.hasOwnProperty(prop) && prevAcc && prevAcc[prop] ) {
				addto(prop, (acc[prop] - prevAcc[prop]), aobj, rec.percent);
			}
		}
	}
	inst = rec.jdata.i;
	if ( inst ) {
		// instant data
		for ( prop in inst ) {
			if ( !inst.hasOwnProperty(prop) ) {
				continue;
			}
			addto(prop, inst[prop], iobj, rec.percent, iobjCounts);
			if ( prevRec && hourFill[prop] ) {
				// calculate hour value, if not already defined for given property
				propHour = hourFill[prop];
				if ( !(acc && acc[propHour]) && prevInst && prevInst[prop] ) {
					addto(propHour, calculateAverageOverHours(inst[prop], prevInst[prop], rec.tdiffms), aobj, rec.percent);
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

return merge(calculateAverages(iobj, iobjCounts), aobj);
$BODY$
  LANGUAGE plv8 VOLATILE;
