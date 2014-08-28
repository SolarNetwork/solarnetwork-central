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
	find the next datum, but only look up to 1 hour after
	this query also handles the situation where there is no data later than the end of the specified time span,
	e.g. at the very end of the data
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
	sobj = {},
	robj,
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
	
while ( rec = cur.fetch() ) {
	if ( !rec.jdata ) {
		continue;
	}
	acc = rec.jdata.a;
	if ( prevRec && acc ) {
		// accumulating data
		for ( prop in acc ) {
			if ( acc.hasOwnProperty(prop) && prevAcc && prevAcc[prop] !== undefined ) {
				sn.math.util.addto(prop, (acc[prop] - prevAcc[prop]), aobj, rec.percent);
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

robj = sn.util.merge(sn.math.util.calculateAverages(iobj, iobjCounts), aobj);

// merge last record s obj into results, but not overwriting any existing properties
if ( prevRec && prevRec.percent > 0 && prevRec.jdata.s ) {
	robj = sn.util.merge(prevRec.jdata.s, robj);
}

return robj;
$BODY$
  LANGUAGE plv8 STABLE;


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
		ELSE
			agg_span := interval '1 day';
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
				ELSE
					DELETE FROM solaragg.agg_datum_daily
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
				ELSE
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
