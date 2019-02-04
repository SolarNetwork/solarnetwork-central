CREATE SCHEMA IF NOT EXISTS solarcommon;

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

CREATE OR REPLACE FUNCTION solarcommon.plainto_prefix_tsquery(config regconfig, qtext TEXT)
RETURNS tsquery AS $$
SELECT to_tsquery(config,
	regexp_replace(
			regexp_replace(
				regexp_replace(qtext, E'[^\\w ]', '', 'g'),
			E'\\M', ':*', 'g'),
		E'\\s+',' & ','g')
);
$$ LANGUAGE SQL STRICT IMMUTABLE;

CREATE OR REPLACE FUNCTION solarcommon.plainto_prefix_tsquery(qtext TEXT)
RETURNS tsquery AS $$
SELECT solarcommon.plainto_prefix_tsquery(get_current_ts_config(), qtext);
$$ LANGUAGE SQL STRICT IMMUTABLE;

/**
 * Convert a JSON array into an array of text.
 *
 * @param jdata the JSON array value to convert
 * @returns text array, or NULL if jdata is NULL
 */
CREATE OR REPLACE FUNCTION solarcommon.json_array_to_text_array(jdata jsonb)
   RETURNS text[] LANGUAGE sql IMMUTABLE AS
$$
SELECT
	CASE
		WHEN jdata IS NULL THEN NULL::text[]
		ELSE ARRAY(SELECT jsonb_array_elements_text(jdata))
	END
$$;

CREATE OR REPLACE FUNCTION solarcommon.json_array_to_text_array(jdata json)
   RETURNS text[] LANGUAGE sql IMMUTABLE AS
$$
SELECT
	CASE
		WHEN jdata IS NULL THEN NULL::text[]
		ELSE ARRAY(SELECT json_array_elements_text(jdata))
	END
$$;

/**
 * Combine "jdata" components into a single "jdata" JSON object.
 *
 * @param jdata_i the instantaneous JSON object
 * @param jdata_a the accumulating JSON object
 * @param jdata_s the status JSON object
 * @param jdata_t the tag array
 * @returns JSON object
 */
CREATE OR REPLACE FUNCTION solarcommon.jdata_from_components(
		jdata_i jsonb,
		jdata_a jsonb,
		jdata_s jsonb,
		jdata_t text[])
	RETURNS jsonb
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jsonb_strip_nulls(jsonb_build_object('i', jdata_i, 'a', jdata_a, 's', jdata_s, 't', to_jsonb(jdata_t)));
$$;

/**
 * Split a "jdata" JSON object into components.
 *
 * @param jdata the "jdata" JSON object
 * @returns the component values
 */
CREATE OR REPLACE FUNCTION solarcommon.components_from_jdata(
	IN jdata jsonb,
	OUT jdata_i jsonb,
	OUT jdata_a jsonb,
	OUT jdata_s jsonb,
	OUT jdata_t text[])
	LANGUAGE SQL IMMUTABLE AS
$$
SELECT jdata->'i', jdata->'a', jdata->'s', solarcommon.json_array_to_text_array(jdata->'t')
$$;

/** JSONB number sum aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_sum_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	return (!agg_state ? el : agg_state + el);
$$;

/**
 * Sum aggregate for JSON number values, resulting in a JSON number.
 */
CREATE AGGREGATE solarcommon.jsonb_sum(jsonb) (
    sfunc = solarcommon.jsonb_sum_sfunc,
    stype = jsonb
);

/** JSONB object sum aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_sum_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var addTo,
		prop;
	if ( !agg_state ) {
		agg_state = el;
	} else if ( el ) {
		addTo = require('util/addTo').default;
		for ( prop in el ) {
			addTo(prop, el[prop], agg_state);
		}
	}
	return agg_state;
$$;

/**
 * Sum aggregate for JSON object values, resulting in JSON object.
 *
 * This aggregate will sum the _properties_ of JSON objects, resulting in a JSON object.
 * For example, if aggregating objects like:
 *
 *     {"watts":123, "wattHours":234}
 *     {"watts":234, "wattHours":345}
 *
 * the resulting object would be:
 *
 *    {"watts":357, "wattHours":579}
 */
CREATE AGGREGATE solarcommon.jsonb_sum_object(jsonb) (
    sfunc = solarcommon.jsonb_sum_object_sfunc,
    stype = jsonb
);

/** JSONB number average aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_avg_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	if ( !agg_state ) {
		return {s:el,c:(el !== null ? 1 : 0)};
	}
	agg_state.s += el;
	if ( el !== null ) {
		agg_state.c += 1;
	}
	return agg_state;
$$;

/** JSONB number average aggregate final calculation function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_avg_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
    return (agg_state.c > 0 ? agg_state.s / agg_state.c : null);
$$;

/**
 * Average aggregate for JSON number values, resulting in a JSON number.
 */
CREATE AGGREGATE solarcommon.jsonb_avg(jsonb) (
    sfunc = solarcommon.jsonb_avg_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_avg_finalfunc
);

/** JSONB object average aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_avg_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var addTo,
		prop,
		d,
		c;
	if ( !agg_state ) {
		c = {};
		for ( prop in el ) {
			c[prop] = 1;
		}
		agg_state = {d:el, c:c};
	} else if ( el ) {
		addTo = require('util/addTo').default;
		d = agg_state.d;
		c = agg_state.c;
		for ( prop in el ) {
			addTo(prop, el[prop], d, 1, c);
		}
	}
	return agg_state;
$$;

/** JSONB object average aggregate final calculation function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_avg_object_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var calculateAverages = require('math/calculateAverages').default;
	return calculateAverages(agg_state.d, agg_state.c);
$$;

/**
 * Average aggregate for JSON object values, resulting in JSON object.
 *
 * This aggregate will sum the _properties_ of JSON objects, resulting in a JSON object.
 * For example, if aggregating objects like:
 *
 *     {"watts":123, "wattHours":234}
 *     {"watts":234, "wattHours":345}
 *
 * the resulting object would be:
 *
 *    {"watts":178.5, "wattHours":289.5}
 */
CREATE AGGREGATE solarcommon.jsonb_avg_object(jsonb) (
    sfunc = solarcommon.jsonb_avg_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_avg_object_finalfunc
);

/**
 * Format a timestamp to RFC 1123 syntax using the GMT time zone.
 *
 * This is the form used by SNWS2 signatures and HTTP date headers.
 *
 * @param d the timestamp to format
 * @returns the formatted date, e.g. `Tue, 25 Apr 2017 14:30:00 GMT`
 */
CREATE OR REPLACE FUNCTION solarcommon.to_rfc1123_utc(d timestamptz)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT to_char(d at time zone 'UTC', 'Dy, DD Mon YYYY HH24:MI:SS "GMT"');
$$;

/**
 * Convert an Ant path pattern into a regular expression.
 *
 * This can be used to convert source ID wildcard patterns into
 * regular expressions that Postgres understands.
 *
 * @param pat the Ant path pattern to convert
 * @returns the equivalent regular expression
 */
CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_to_regexp(pat text)
RETURNS text LANGUAGE SQL STRICT IMMUTABLE AS
$$
	SELECT '^' ||
		regexp_replace(
			regexp_replace(
				regexp_replace(
					regexp_replace(pat, '([!$()+.:<=>[\\\]^{|}-])', '\\\1', 'g'),
				E'[?]', E'[^/]', 'g'),
			E'(?<![*])[*](?![*])', E'[^/]*', 'g'),
		E'[*]{2}', '(?<=/|^).*(?=/|$)', 'g')
		|| '$';
$$;

/** Aggregate helper function that always returns the first non-NULL item. */
CREATE OR REPLACE FUNCTION solarcommon.first_sfunc(anyelement, anyelement)
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
CREATE AGGREGATE solarcommon.first(
	sfunc    = solarcommon.first_sfunc,
	basetype = anyelement,
    stype    = anyelement
);

/** JSONB object diff aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diff_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop,
		f,
		curr;
	if ( !agg_state && el ) {
		agg_state = {first:el, last:el};
	} else if ( el ) {
		f = agg_state.first;
		curr = agg_state.last;
		for ( prop in el ) {
			curr[prop] = el[prop];
			if ( f[prop] === undefined ) {
				// property discovered mid-way while aggregating; add to "first" now
				f[prop] = el[prop];
			}
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
		f = (agg_state ? agg_state.first : null),
		l = (agg_state ? agg_state.last : null),
		r;
	if ( l ) {
		r = {};
		for ( prop in l ) {
			val = f[prop];
			if ( val !== undefined ) {
				r[prop +'_start'] = val;
				r[prop +'_end'] = l[prop];
				r[prop] = l[prop] - val;
			}
		}
	} else {
		r = null;
	}
    return r;
$$;

/**
 * Difference aggregate for JSON object values, resulting in a JSON object.
 *
 * This aggregate will subtract the _property values_ of the first JSON object in the aggregate group
 * from the last object in the group, resulting in a JSON object. An `ORDER BY` clause is thus essential
 * to ensure the first/last values are captured correctly. The first and last values for each property
 * will be included in the output JSON object with `_start` and `_end` suffixes added to their names.
 *
 * For example, if aggregating objects like:
 *
 *     {"wattHours":234}
 *     {"wattHours":346}
 *
 * the resulting object would be:
 *
 *    {"wattHours":112, "wattHours_start":234, "wattHours_end":346}
 */
CREATE AGGREGATE solarcommon.jsonb_diff_object(jsonb) (
    sfunc = solarcommon.jsonb_diff_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_diff_object_finalfunc
);

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
 * Cast a JSON array to a BIGINT array.
 *
 * @param json the JSON array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.json_array_to_bigint_array(json)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM json_array_elements_text($1) t(x);
$$;

/**
 * Cast a JSONB array to a BIGINT array.
 *
 * @param jsonb the JSONB array to cast
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_array_to_bigint_array(jsonb)
RETURNS bigint[] LANGUAGE sql IMMUTABLE AS $$
    SELECT array_agg(x)::bigint[] || ARRAY[]::bigint[] FROM jsonb_array_elements_text($1) t(x);
$$;

