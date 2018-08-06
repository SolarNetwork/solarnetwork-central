/** Aggregate helper function that always returns the first non-NULL item. */
CREATE OR REPLACE FUNCTION solarcommon.first_sfunc (anyelement, anyelement)
RETURNS anyelement LANGUAGE SQL IMMUTABLE STRICT AS $$
    SELECT $1;
$$;

/**
 * First aggregate value.
 *
 * This aggregate will return the first value encountered in the aggregate group. Given
 * the order within the group is generally undefined, using an `ORDER BY` clause is usually
 * needed. For example, to select the first timestamp for each datum by node and source:
 *
 * 		SELECT solarcommon.first(ts ORDER BY ts) AS ts_start
 * 		FROM solardatum.da_datum
 * 		GROUP BY node_id, source_id
 */
CREATE AGGREGATE solarcommon.first (
	sfunc    = solarcommon.first_sfunc,
	basetype = anyelement,
    stype    = anyelement
);

/** JSONB object diff aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diff_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop,
		curr;
	if ( !agg_state ) {
		agg_state = {first:el, last:el};
	} else if ( el ) {
		curr = agg_state.last;
		for ( prop in el ) {
			curr[prop] = el[prop];
		}
	}
	return agg_state;
$$;

/** JSONB object diff aggregate final calculation function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diff_object_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop,
		val,
		f = agg_state.first,
		l = agg_state.last,
		r = {};
	for ( prop in l ) {
		val = f[prop];
		if ( val !== undefined ) {
			r[prop] = l[prop] - val;
		}
	}
    return r;
$$;

/**
 * Difference aggregate for JSON object values, resulting in JSON object.
 *
 * This aggregate will subjtract the _properties_ of the first JSON object from the last
 * JSON object, resulting in a JSON object. For example, if aggregating objects like:
 *
 *     {"watts":123, "wattHours":234}
 *     {"watts":234, "wattHours":346}
 *
 * the resulting object would be:
 *
 *    {"watts":111, "wattHours":112}
 */
CREATE AGGREGATE solarcommon.jsonb_diff_object(jsonb) (
    sfunc = solarcommon.jsonb_diff_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_diff_object_finalfunc
);

/* time weighted json object */

/** JSONB object average aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_weighted_proj_object_sfunc(agg_state jsonb, el jsonb, weight float8)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop;
	if ( !agg_state ) {
		agg_state = {weight:weight, first:el};
	} else if ( el ) {
		agg_state.last = el;
	}
	return agg_state;
$$;

/** JSONB object average aggregate final calculation function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_weighted_proj_object_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var w = agg_state.weight,
		f = agg_state.first,
		l = agg_state.last,
		prop,
		firstVal,
		res = {};
	if ( !(f && l) ) {
		return f;
	}
	for ( prop in l ) {
		firstVal = f[prop];
		if ( firstVal ) {
			res[prop] = firstVal + ((l[prop] - firstVal) * w);
		}
	}
	return res;
$$;

/**
 * Weighted projection for JSON object values, resulting in a JSON object.
 *
 * This aggregate will project all _properties_ of a JSON object between the *first* and *last* values of each property,
 * multiplied by a weight (e.g. a percentage from 0 to 1), resulting in a JSON object.
 *
 * For example, if aggregating objects like:
 *
 *     {"wattHours":234}
 *     {"wattHours":345}
 *
 * with a call like `solarcommon.jsonb_weighted_proj_object(jdata_a, 0.25)`
 *
 * the resulting object would be:
 *
 *    {"wattHours":261.75}
 *
 * because the calculation is 345 + (345 - 234) * 0.25.
 */
CREATE AGGREGATE solarcommon.jsonb_weighted_proj_object(jsonb, float8) (
    sfunc = solarcommon.jsonb_weighted_proj_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_weighted_proj_object_finalfunc
);

/**
 * Project the values of a datum at a specific point in time, by deriving from the previous and next values
 * from the same source ID.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts` column will
 * simply be `reading_ts`. The `jdata_i` column will be computed as an average of the previous/next rows,
 * and `jdata_a` will be time-projected based on the previous/next readings.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param reading_ts	the timestamp to calculate the value of each datum at
 * @param span			a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_at(nodes bigint[], sources text[], reading_ts timestamptz, span interval)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id text,
  jdata_i jsonb,
  jdata_a jsonb
) LANGUAGE SQL STABLE AS $$
	WITH slice AS (
		SELECT
			d.ts,
			CASE
				WHEN d.ts <= reading_ts THEN last_value(d.ts) OVER win
				ELSE first_value(d.ts) OVER win
			END AS slot_ts,
			lead(d.ts) OVER win_full AS next_ts,
			EXTRACT(epoch FROM (reading_ts - d.ts))
				/ EXTRACT(epoch FROM (lead(d.ts) OVER win_full - d.ts)) AS weight,
			d.node_id,
			d.source_id,
			d.jdata_i,
			d.jdata_a
		FROM solardatum.da_datum d
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts >= reading_ts - span
			AND d.ts < reading_ts + span
		WINDOW win AS (PARTITION BY d.node_id, d.source_id, CASE WHEN d.ts <= reading_ts
			THEN 0 ELSE 1 END ORDER BY d.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			win_full AS (PARTITION BY d.node_id, d.source_id)
		ORDER BY d.node_id, d.source_id, d.ts
	)
	SELECT reading_ts AS ts,
		node_id,
		source_id,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN reading_ts THEN solarcommon.first(jdata_i ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_avg_object(jdata_i)
		END AS jdata_i,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN reading_ts THEN solarcommon.first(jdata_a ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_weighted_proj_object(jdata_a, weight)
		END AS jdata_a
	FROM slice
	WHERE ts = slot_ts
	GROUP BY node_id, source_id
	HAVING count(*) > 1 OR solarcommon.first(ts ORDER BY ts) = reading_ts OR solarcommon.first(ts ORDER BY ts DESC) = reading_ts
	ORDER BY node_id, source_id
$$;

/**
 * Project the values of a datum at a specific point in node-local time, by deriving from the previous and next values
 * from the same source ID.
 *
 * This returns one row per node ID and source ID combination found. The returned `ts` column will
 * be `reading_ts` at the time zone for each node. The `jdata_i` column will be computed as an average of the previous/next rows,
 * and `jdata_a` will be time-projected based on the previous/next readings.
 *
 * @param nodes 		the node IDs to find
 * @param sources 		the source IDs to find
 * @param reading_ts	the timestamp to calculate the value of each datum at
 * @param span			a maximum range before and after `reading_ts` to consider when looking for the previous/next datum
 */
CREATE OR REPLACE FUNCTION solardatum.calculate_datum_at_local(nodes bigint[], sources text[], reading_ts timestamp, span interval)
RETURNS TABLE(
  ts timestamp with time zone,
  node_id bigint,
  source_id text,
  jdata_i jsonb,
  jdata_a jsonb
) LANGUAGE SQL STABLE AS $$
	WITH t AS (
		SELECT node_id, reading_ts AT TIME ZONE time_zone AS ts
		FROM solarnet.node_local_time
		WHERE node_id = ANY(nodes)
	), slice AS (
		SELECT
			d.ts,
			t.ts AS ts_slot,
			CASE
				WHEN d.ts <= t.ts THEN last_value(d.ts) OVER win
				ELSE first_value(d.ts) OVER win
			END AS slot_ts,
			lead(d.ts) OVER win_full AS next_ts,
			EXTRACT(epoch FROM (t.ts - d.ts))
				/ EXTRACT(epoch FROM (lead(d.ts) OVER win_full - d.ts)) AS weight,
			d.node_id,
			d.source_id,
			d.jdata_i,
			d.jdata_a
		FROM solardatum.da_datum d
		INNER JOIN t ON t.node_id = d.node_id
		WHERE d.node_id = ANY(nodes)
			AND d.source_id = ANY(sources)
			AND d.ts >= t.ts - span
			AND d.ts < t.ts + span
		WINDOW win AS (PARTITION BY d.node_id, d.source_id, CASE WHEN d.ts <= t.ts
			THEN 0 ELSE 1 END ORDER BY d.ts RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING),
			win_full AS (PARTITION BY d.node_id, d.source_id)
		ORDER BY d.node_id, d.source_id, d.ts
	)
	SELECT
		ts_slot AS ts,
		node_id,
		source_id,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN ts_slot THEN solarcommon.first(jdata_i ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_avg_object(jdata_i)
		END AS jdata_i,
		CASE solarcommon.first(ts ORDER BY ts)
			-- we have exact timestamp (improbable!)
			WHEN ts_slot THEN solarcommon.first(jdata_a ORDER BY ts)

			-- more likely, project prop values based on linear difference between start/end samples
			ELSE solarcommon.jsonb_weighted_proj_object(jdata_a, weight)
		END AS jdata_a
	FROM slice
	WHERE ts = slot_ts
	GROUP BY ts_slot, node_id, source_id
	HAVING count(*) > 1 OR solarcommon.first(ts ORDER BY ts) = ts_slot OR solarcommon.first(ts ORDER BY ts DESC) = ts_slot
	ORDER BY ts_slot, node_id, source_id
$$;
