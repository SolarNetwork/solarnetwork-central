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
		agg_state = {d:(el !== null ? el : {}), c:c};
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
