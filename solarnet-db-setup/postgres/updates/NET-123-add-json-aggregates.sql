CREATE OR REPLACE FUNCTION solarnet.sn_jsonb_sum_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	plv8.elog(NOTICE, 'agg_state = ', agg_state, ', el = ', el)
	return (agg_state === undefined ? el : agg_state + el);
$$;

CREATE OR REPLACE FUNCTION solarnet.sn_jsonb_sum_finalfunc(agg_state jsonb) 
RETURNS DOUBLE PRECISION LANGUAGE plv8 IMMUTABLE AS $$
    return agg_state;
$$;

CREATE AGGREGATE solarnet.sn_jsonb_sum(jsonb) (
    sfunc = solarnet.sn_jsonb_sum_sfunc,
    stype = jsonb,
    finalfunc = solarnet.sn_jsonb_sum_finalfunc
);
