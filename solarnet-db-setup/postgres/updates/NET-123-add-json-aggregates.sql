/* sum json number */

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

/* sum json object */

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

/* average json number */

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

/* average json object */

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
