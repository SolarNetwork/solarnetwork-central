/** JSONB object subtract aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_sub_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var addTo,
		prop;
	if ( !agg_state ) {
		agg_state = el;
	} else if ( el ) {
		addTo = require('util/addTo').default;
		for ( prop in el ) {
			addTo(prop, el[prop], agg_state, -1);
		}
	}
	return agg_state;
$$;

/**
 * Subtract aggregate for JSON object values, resulting in JSON object.
 *
 * This aggregate will subtract the _properties_ of JSON objects, resulting in a JSON object.
 * For example, if aggregating objects like:
 *
 *     {"watts":234, "wattHours":345}
 *     {"watts":123, "wattHours":222}
 *
 * the resulting object would be:
 *
 *    {"watts":111, "wattHours":123}
 */
CREATE AGGREGATE solarcommon.jsonb_sub_object(jsonb) (
    sfunc = solarcommon.jsonb_sub_object_sfunc,
    stype = jsonb
);
