-- update to simplify by eliminating exceptions in favor of ON CONFLICT and supporting marking 
-- any "next" time slot as stale (instead of only next hour) to handle reading re-calculations
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

-- also use same stale data population trigger on aux data
CREATE TRIGGER aa_agg_stale_datum_aux
    BEFORE INSERT OR DELETE OR UPDATE 
    ON solardatum.da_datum_aux
    FOR EACH ROW
    EXECUTE PROCEDURE solardatum.trigger_agg_stale_datum();

-- update to simplify by eliminating exceptions in favor of ON CONFLICT
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



-- ======================
-- agg

/** JSONB object diffsum aggregate final calculation function for jdata result structure. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diffsum_jdata_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop,
		val,
		f = (agg_state ? agg_state.first : null),
		p = (agg_state ? agg_state.prev : null),
		t = (agg_state ? agg_state.total : null),
		l = (agg_state ? agg_state.last : null);
	if ( p ) {
		for ( prop in p ) {
			if ( t[prop] === undefined ) {
				t[prop] = 0;
			}
		}
	}
	
	for ( prop in t ) {
		return {'a':t, 'af':l, 'as':f};
	}
    return null;
$$;

/**
 * Difference and sum aggregate for JSON object values, resulting in a JSON object.
 *
 * This aggregate will subtract the _property values_ of the odd JSON objects in the aggregate group
 * from the next even object in the group, resulting in a JSON object. Each pair or objects are then 
 * added together to form a final aggregate value. An `ORDER BY` clause is thus essential
 * to ensure the odd/even values are captured correctly.
 *
 * The difference result object will be returned under a `a` property. The first and last values for
 * each property will be included in the output JSON object under `as` and `af` properties.
 *
 * For example, if aggregating objects like:
 *
 *     {"wattHours":234}
 *     {"wattHours":346}
 *     {"wattHours":1000}
 *     {"wattHours":1100}
 *
 * the resulting object would be:
 *
 *    {"a":{"wattHours":212}, "as":{"wattHours_start":234}, "af":{"wattHours_end":1100}}
 */
CREATE AGGREGATE solarcommon.jsonb_diffsum_jdata(jsonb) (
    sfunc = solarcommon.jsonb_diffsum_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_diffsum_jdata_finalfunc
);

-- agg
-- ======================


/**
 * Calculate the difference between the accumulating properties of datum over a time range.
 *
 * This returns at most one row. The returned `ts_start` and `ts_end` columns will
 * the timestamps of the found starting/ending datum records. The `jdata` column will be computed as the difference
 * between the starting/ending rows, using the `solarcommon.jsonb_diffsum_jdata()` aggregate function.
 *
 * @param node 			the node ID to find
 * @param source 		the source ID to find
 * @param ts_min		the timestamp of the start of the time range (inclusive)
 * @param ts_max		the timestamp of the end of the time range (exclusive)
 * @param tolerance 	the maximum time span to look backwards for the previous reading record; smaller == faster
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	node bigint, source text, ts_min timestamptz, ts_max timestamptz, tolerance interval default interval '3 months')
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata jsonb
) LANGUAGE sql STABLE ROWS 1 AS $$
	WITH latest_before_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_min
					AND ts >= ts_min - tolerance
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, earliest_after_start AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find earliest on/after
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
				ORDER BY ts 
				LIMIT 1
			)
			UNION ALL
			(
				-- find earliest on/after reset
				SELECT ts, node_id, source_id, jdata_as AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts >= ts_min
					AND ts < ts_max
				ORDER BY ts
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts, rr DESC
		LIMIT 1
	)
	, latest_before_end AS (
		SELECT ts, node_id, source_id, jdata_a FROM (
			(
				-- find latest before
				SELECT ts, node_id, source_id, jdata_a, 0 AS rr
				FROM solardatum.da_datum
				WHERE node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
				ORDER BY ts DESC 
				LIMIT 1
			)
			UNION ALL
			(
				-- find latest before reset
				SELECT ts, node_id, source_id, jdata_af AS jdata_a, 1 AS rr
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = node
					AND source_id = source
					AND ts < ts_max
					AND ts >= ts_min
				ORDER BY ts DESC
				LIMIT 1
			)
		) d
		-- add order by rr so that when datum & reset have equivalent ts, reset has priority
		ORDER BY d.ts DESC, rr DESC
		LIMIT 1
	)
	, d AS (
		(
			SELECT *
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.ts
			LIMIT 1
		)
		UNION ALL
		(
			SELECT * FROM latest_before_end
		)
	)
	, ranges AS (
		SELECT min(ts) AS sdate
			, max(ts) AS edate
		FROM d
	)
	, combined AS (
		SELECT * FROM d
	
		UNION ALL
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges, solardatum.da_datum_aux aux 
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
			AND aux.node_id = node 
			AND aux.source_id = source
			AND aux.ts > ranges.sdate
			AND aux.ts < ranges.edate
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_jdata(d.jdata_a ORDER BY d.ts) AS jdata
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
$$;


-- change time zone to left outer join, defaulting to UTC if not available
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_diff_over(
	nodes bigint[], sources text[], ts_min timestamptz, ts_max timestamptz)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_id bigint,
  source_id character varying(64),
  jdata_a jsonb
) LANGUAGE sql STABLE AS $$
	-- find records closest to, but not after, min date
	-- also considering reset records, using their STARTING sample value
	WITH latest_before_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			SELECT d.ts, d.node_id, d.source_id, d.jdata_a
			FROM  solardatum.find_latest_before(nodes, sources, ts_min) dates
			INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			UNION
			SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
			FROM solardatum.da_datum_aux
			WHERE atype = 'Reset'::solardatum.da_datum_aux_type
				AND node_id = ANY(nodes)
				AND source_id = ANY(sources)
				AND ts < ts_min
			ORDER BY node_id, source_id, ts DESC
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- in case no data before min date, find closest to min date or after
	-- also considering reset records, using their STARTING sample value
	, earliest_after_start AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_earliest_after(nodes, sources, ts_min) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_as AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts >= ts_min
				ORDER BY node_id, source_id, ts
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts
	)
	-- find records closest to, but not after max date (could be same as latest_before_start or earliest_after_start)
	-- also considering reset records, using their FINAL sample value
	, latest_before_end AS (
		SELECT DISTINCT ON (d.node_id, d.source_id) d.*
		FROM (
			(
				SELECT d.ts, d.node_id, d.source_id, d.jdata_a
				FROM solardatum.find_latest_before(nodes, sources, ts_max) dates
				INNER JOIN solardatum.da_datum d ON d.ts = dates.ts AND d.node_id = dates.node_id AND d.source_id = dates.source_id
			)
			UNION
			(
				SELECT DISTINCT ON (node_id, source_id) ts, node_id, source_id, jdata_af AS jdata_a
				FROM solardatum.da_datum_aux
				WHERE atype = 'Reset'::solardatum.da_datum_aux_type
					AND node_id = ANY(nodes)
					AND source_id = ANY(sources)
					AND ts < ts_max
				ORDER BY node_id, source_id, ts DESC
			)
		) d
		ORDER BY d.node_id, d.source_id, d.ts DESC
	)
	-- narrow data to [start, final] pairs of rows by node,source by choosing
	-- latest_before_start in preference to earliest_after_start
	, d AS (
		SELECT * FROM (
			SELECT DISTINCT ON (d.node_id, d.source_id) d.*
			FROM (
				SELECT * FROM latest_before_start
				UNION
				SELECT * FROM earliest_after_start
			) d
			ORDER BY d.node_id, d.source_id, d.ts
		) earliest
		UNION 
		SELECT * FROM latest_before_end
	)
	-- begin search for reset records WITHIN [start, final] date ranges via table of found [start, final] dates
	, ranges AS (
		SELECT node_id
			, source_id
			, min(ts) AS sdate
			, max(ts) AS edate
		FROM d
		GROUP BY node_id, source_id
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.node_id
			, aux.source_id
			, unnest(ARRAY[aux.jdata_af, aux.jdata_as]) AS jdata_a
		FROM ranges
		INNER JOIN solardatum.da_datum_aux aux ON aux.node_id = ranges.node_id AND aux.source_id = ranges.source_id
			AND aux.ts > ranges.sdate AND aux.ts < ranges.edate
		WHERE atype = 'Reset'::solardatum.da_datum_aux_type
	)
	-- combine [start, final] pairs with reset pairs
	, combined AS (
		SELECT * FROM d
		UNION
		SELECT * FROM resets
	)
	-- calculate difference by node,source, of {start[, resetFinal1, resetStart1, ...], final}
	SELECT min(d.ts) AS ts_start,
		max(d.ts) AS ts_end,
		min(COALESCE(nlt.time_zone, 'UTC')) AS time_zone,
		d.node_id,
		d.source_id,
		solarcommon.jsonb_diffsum_object(d.jdata_a ORDER BY d.ts) AS jdata_a
	FROM combined d
	LEFT OUTER JOIN solarnet.node_local_time nlt ON nlt.node_id = d.node_id
	GROUP BY d.node_id, d.source_id
	ORDER BY d.node_id, d.source_id
$$;


ALTER TABLE solaragg.agg_datum_hourly 
	ADD COLUMN jdata_as jsonb
	, ADD COLUMN jdata_af jsonb
	, ADD COLUMN jdata_ad jsonb;

ALTER TABLE solaragg.agg_datum_daily 
	ADD COLUMN jdata_as jsonb
	, ADD COLUMN jdata_af jsonb
	, ADD COLUMN jdata_ad jsonb;

ALTER TABLE solaragg.agg_datum_monthly 
	ADD COLUMN jdata_as jsonb
	, ADD COLUMN jdata_af jsonb
	, ADD COLUMN jdata_ad jsonb;



CREATE OR REPLACE FUNCTION solaragg.process_one_agg_stale_datum(kind char)
  RETURNS integer LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	stale 					record;
	agg_span 				interval;
	agg_json 				jsonb := NULL;
	agg_jmeta 				jsonb := NULL;
	agg_reading 			jsonb := NULL;
	agg_reading_ts_start 	timestamptz := NULL;
	agg_reading_ts_end 		timestamptz := NULL;
	node_tz 				text := 'UTC';
	proc_count 				integer := 0;
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

		CASE kind
			WHEN 'h' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_datum_time_slots(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, agg_span, 0, interval '1 hour')
				INTO agg_json, agg_jmeta;
				
				SELECT jdata, ts_start, ts_end
				FROM solardatum.calculate_datum_diff_over(stale.node_id, stale.source_id::text, stale.ts_start, stale.ts_start + agg_span)
				INTO agg_reading, agg_reading_ts_start, agg_reading_ts_end;

			WHEN 'd' THEN
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_datum_agg(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale.ts_start + agg_span, 'h')
				INTO agg_json, agg_jmeta;
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_hourly
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < (stale.ts_start + agg_span)
				GROUP BY node_id, source_id
				INTO agg_reading;

			ELSE
				SELECT jdata, jmeta
				FROM solaragg.calc_agg_datum_agg(stale.node_id, ARRAY[stale.source_id::text], stale.ts_start, stale.ts_start + agg_span, 'd')
				INTO agg_json, agg_jmeta;
				
				SELECT jsonb_strip_nulls(jsonb_build_object(
					 'as', solarcommon.first(jdata_as ORDER BY ts_start),
					 'af', solarcommon.first(jdata_af ORDER BY ts_start DESC),
					 'a', solarcommon.jsonb_sum_object(jdata_ad)
				))
				FROM solaragg.agg_datum_daily
				WHERE node_id = stale.node_id
					AND source_id = stale.source_id
					AND ts_start >= stale.ts_start
					AND ts_start < (stale.ts_start + agg_span)
				GROUP BY node_id, source_id
				INTO agg_reading;
		END CASE;

		IF agg_json IS NULL AND (agg_reading IS NULL 
				OR (agg_reading_ts_start IS NOT NULL AND agg_reading_ts_start = agg_reading_ts_end)
				) THEN
			-- delete agg, using date range in case time zone of node has changed
			CASE kind
				WHEN 'h' THEN
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
				WHEN 'd' THEN
					DELETE FROM solaragg.agg_datum_daily
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
				ELSE
					DELETE FROM solaragg.agg_datum_monthly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span;
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
						stale.ts_start at time zone node_tz,
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
					DELETE FROM solaragg.agg_datum_hourly
					WHERE node_id = stale.node_id
						AND source_id = stale.source_id
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
				WHEN 'd' THEN
					INSERT INTO solaragg.agg_datum_daily (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone node_tz AS DATE),
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
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
				ELSE
					INSERT INTO solaragg.agg_datum_monthly (
						ts_start, local_date, node_id, source_id,
						jdata_i, jdata_a, jdata_s, jdata_t, jmeta,
						jdata_as, jdata_af, jdata_ad)
					VALUES (
						stale.ts_start,
						CAST(stale.ts_start at time zone node_tz AS DATE),
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
						AND ts_start > stale.ts_start - agg_span
						AND ts_start < stale.ts_start + agg_span
						AND ts_start <> stale.ts_start;
			END CASE;
		END IF;
		DELETE FROM solaragg.agg_stale_datum WHERE CURRENT OF curs;
		proc_count := 1;

		-- now make sure we recalculate the next aggregate level by submitting a stale record for the next level
		-- and also update daily audit stats
		CASE kind
			WHEN 'h' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;

			WHEN 'd' THEN
				INSERT INTO solaragg.agg_stale_datum (ts_start, node_id, source_id, agg_kind)
				VALUES (date_trunc('month', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;

				-- handle update to raw audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'r')
				ON CONFLICT DO NOTHING;

				-- handle update to hourly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'h')
				ON CONFLICT DO NOTHING;

				-- handle update to daily audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('day', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'd')
				ON CONFLICT DO NOTHING;
			ELSE
				-- handle update to monthly audit data
				INSERT INTO solaragg.aud_datum_daily_stale (ts_start, node_id, source_id, aud_kind)
				VALUES (date_trunc('month', stale.ts_start at time zone node_tz) at time zone node_tz, stale.node_id, stale.source_id, 'm')
				ON CONFLICT DO NOTHING;
		END CASE;
	END IF;
	CLOSE curs;
	RETURN proc_count;
END;
$$;

CREATE OR REPLACE FUNCTION solaruser.expire_datum_for_policy(userid bigint, jpolicy jsonb, age interval)
  RETURNS bigint LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	total_count bigint := 0;
	one_count bigint := 0;
	node_ids bigint[];
	have_source_ids boolean := jpolicy->'sourceIds' IS NULL;
	source_id_regexs text[];
	agg_key text := jpolicy->>'aggregationKey';
BEGIN
	-- filter node IDs to only those owned by user
	SELECT ARRAY(SELECT node_id
				 FROM solaruser.user_node un
				 WHERE un.user_id = userid
					AND (
						jpolicy->'nodeIds' IS NULL
						OR jpolicy->'nodeIds' @> un.node_id::text::jsonb
					)
				)
	INTO node_ids;

	-- get array of source ID regexs
	SELECT ARRAY(SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(jpolicy->'sourceIds')))
	INTO source_id_regexs;

	-- delete raw data
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	DELETE FROM solardatum.da_datum d
	USING nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts < nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- delete any triggered stale rows
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	DELETE FROM solaragg.agg_stale_datum d
	USING nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts_start <= nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		AND d.agg_kind = 'h';

	-- update daily audit datum counts
	WITH nlt AS (
		SELECT
			node_id,
			(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
		FROM solarnet.node_local_time
		WHERE node_id = ANY (node_ids)
	)
	UPDATE solaragg.aud_datum_daily d
	SET datum_count = 0
	FROM nlt
	WHERE d.node_id = nlt.node_id
		AND d.ts_start < nlt.older_than
		AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));

	IF agg_key IN ('h', 'd', 'M') THEN
		-- delete hourly data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_hourly d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;

		-- update daily audit datum counts
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		UPDATE solaragg.aud_datum_daily d
		SET datum_hourly_count = 0
		FROM nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	END IF;

	IF agg_key IN ('d', 'M') THEN
		-- delete daily data
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_daily d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;

		-- update daily audit datum counts
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		UPDATE solaragg.aud_datum_daily d
		SET datum_daily_pres = FALSE
		FROM nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
	END IF;

	IF agg_key = 'M' THEN
		-- delete monthly data (round down to whole months only)
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		DELETE FROM solaragg.agg_datum_monthly d
		USING nlt
		WHERE d.node_id = nlt.node_id
			AND d.ts_start < older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs));
		GET DIAGNOSTICS one_count = ROW_COUNT;
		total_count := total_count + one_count;
	END IF;

	-- mark all monthly audit data as stale for recalculation
	IF total_count > 0 THEN
		INSERT INTO solaragg.aud_datum_daily_stale (node_id, ts_start, source_id, aud_kind)
		WITH nlt AS (
			SELECT
				node_id,
				(date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE time_zone) - age) AT TIME ZONE time_zone AS older_than
			FROM solarnet.node_local_time
			WHERE node_id = ANY (node_ids)
		)
		SELECT d.node_id, d.ts_start, d.source_id, 'm'
		FROM solaragg.aud_datum_monthly d
		INNER JOIN nlt ON nlt.node_id = d.node_id
		WHERE d.ts_start < nlt.older_than
			AND (have_source_ids OR d.source_id ~ ANY(source_id_regexs))
		ON CONFLICT DO NOTHING;
	END IF;
	
	RETURN total_count;
END;
$$;
