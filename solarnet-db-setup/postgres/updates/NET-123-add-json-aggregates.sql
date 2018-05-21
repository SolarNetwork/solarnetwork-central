CREATE OR REPLACE FUNCTION solarcommon.jsonb_sum_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	return (agg_state === undefined ? el : agg_state + el);
$$;

CREATE OR REPLACE FUNCTION solarcommon.jsonb_sum_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
    return agg_state;
$$;

CREATE AGGREGATE solarcommon.jsonb_sum(jsonb) (
    sfunc = solarcommon.jsonb_sum_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_sum_finalfunc
);

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

CREATE OR REPLACE FUNCTION solarcommon.jsonb_sum_object_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
    return agg_state;
$$;

CREATE AGGREGATE solarcommon.jsonb_sum_object(jsonb) (
    sfunc = solarcommon.jsonb_sum_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_sum_object_finalfunc
);
