CREATE TYPE solardatum.da_datum_aux_type AS ENUM
	('Reset');

/**************************************************************************************************
 * TABLE solardatum.da_datum_aux
 *
 * Holds auxiliary records for datum records, where final/start data is inserted into the data stream.
 * Thus each row contains essentially two datum records. These auxiliary records serve to 
 * help transition the data stream in some way.
 *
 * atype - the auxiliary record type
 * jdata_af - the accumulating data "final" value
 * jdata_as - the accumulating data "start" value
 */
CREATE TABLE solardatum.da_datum_aux (
  ts timestamp with time zone NOT NULL,
  node_id bigint NOT NULL,
  source_id character varying(64) NOT NULL,
  atype solardatum.da_datum_aux_type NOT NULL DEFAULT 'Reset'::solardatum.da_datum_aux_type,
  jdata_af jsonb,
  jdata_as jsonb,
  notes text,
  CONSTRAINT da_datum_aux_pkey PRIMARY KEY (node_id, ts, source_id, atype)
);

/** JSONB object diffsum aggregate state transition function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diffsum_object_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var prop,
		f,
		p,
		l,
		t,
		val;
	if ( !agg_state && el ) {
		agg_state = {first:el, last:el, prev:el, total:{}};
	} else if ( el ) {
		f = agg_state.first;
		p = agg_state.prev;
		t = agg_state.total;
		l = agg_state.last;
		if ( p ) {
			// right-hand side; diff from prev and add to total
			for ( prop in el ) {
				// stash current val on "last" record
				l[prop] = el[prop];
				
				if ( f[prop] === undefined ) {
					// property discovered mid-way while aggregating; add to "first" now
					f[prop] = el[prop];
				}
				if ( p[prop] === undefined ) {
					// property discovered mid-way while aggregating; diff is 0
					val = 0;
				} else {
					val = el[prop] - p[prop];
				}
				if ( t[prop] ) {
					t[prop] += val;
				} else {
					t[prop] = val;
				}
			}
			
			// clear prev record
			delete agg_state.prev;
		} else {
			for ( prop in el ) {
				// stash current val on "last" record
				l[prop] = el[prop];

				if ( f[prop] === undefined ) {
					// property discovered mid-way while aggregating; add to "first" now
					f[prop] = el[prop];
				}
			}

			// stash prev side for next diff
			agg_state.prev = el;
		}
	}
	return agg_state;
$$;

/** JSONB object diffsum aggregate final calculation function. */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_diffsum_object_finalfunc(agg_state jsonb)
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
	
	// add in _start/_end props
	for ( prop in t ) {
		val = f[prop];
		if ( val !== undefined ) {
			t[prop +'_start'] = val;
			t[prop +'_end'] = l[prop];
		}
	}
	for ( prop in t ) {
		return t;
	}
    return null;
$$;

DROP AGGREGATE IF EXISTS solarcommon.jsonb_diffsum_object(jsonb);

/**
 * Difference and sum aggregate for JSON object values, resulting in a JSON object.
 *
 * This aggregate will subtract the _property values_ of the odd JSON objects in the aggregate group
 * from the next even object in the group, resulting in a JSON object. Each pair or objects are then 
 * added together to form a final aggregate value. An `ORDER BY` clause is thus essential
 * to ensure the odd/even values are captured correctly. The first and last values for each property
 * will be included in the output JSON object with `_start` and `_end` suffixes added to their names.
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
 *    {"wattHours":212, "wattHours_start":234, "wattHours_end":1100}
 */
CREATE AGGREGATE solarcommon.jsonb_diffsum_object(jsonb) (
    sfunc = solarcommon.jsonb_diffsum_object_sfunc,
    stype = jsonb,
    finalfunc = solarcommon.jsonb_diffsum_object_finalfunc
);
