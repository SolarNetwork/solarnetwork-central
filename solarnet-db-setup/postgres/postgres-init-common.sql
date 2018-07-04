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
	SELECT to_char(d at time zone 'UTC', 'Dy, FMDD Mon YYYY HH24:MI:SS "GMT"');
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
