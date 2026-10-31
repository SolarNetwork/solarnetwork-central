/*
 * Mark functions that do no more than compute a result from their arguments as PARALLEL SAFE.
 *
 * A single PARALLEL UNSAFE function anywhere in a query rules out a parallel plan for the whole
 * query, even if that function is inlined or has only constant arguments. The default for a
 * function is PARALLEL UNSAFE, so any of these appearing in a query had that effect.
 *
 * ALTER FUNCTION, and CREATE OR REPLACE AGGREGATE with an unchanged signature, keep the existing
 * owner and ACL of each object.
 *
 * The aggs_for_vecs extension marks all of its functions PARALLEL UNSAFE, so anything that uses
 * them must stay that way. That includes solardatm.rollup_agg_data, solardatm.avg_agg_data, and
 * all the functions that use those or the extension directly.
 */

-- ==========================================================================================
-- Utilities
-- ==========================================================================================

ALTER FUNCTION solarcommon.ant_pattern_to_regexp(text) PARALLEL SAFE;
ALTER FUNCTION solarcommon.array_transpose2d(anyarray) PARALLEL SAFE;
ALTER FUNCTION solarcommon.basex_integer(text, integer) PARALLEL SAFE;
ALTER FUNCTION solarcommon.components_from_jdata(jsonb) PARALLEL SAFE;
ALTER FUNCTION solarcommon.jdata_from_components(jsonb, jsonb, jsonb, text[]) PARALLEL SAFE;
ALTER FUNCTION solarcommon.json_array_to_bigint_array(json) PARALLEL SAFE;
ALTER FUNCTION solarcommon.json_array_to_text_array(json) PARALLEL SAFE;
ALTER FUNCTION solarcommon.json_array_to_text_array(jsonb) PARALLEL SAFE;
ALTER FUNCTION solarcommon.jsonb_array_to_bigint_array(jsonb) PARALLEL SAFE;
ALTER FUNCTION solarcommon.plainto_prefix_tsquery(regconfig, text) PARALLEL SAFE;
ALTER FUNCTION solarcommon.plainto_prefix_tsquery(text) PARALLEL SAFE;
ALTER FUNCTION solarcommon.reduce_dim(anyarray) PARALLEL SAFE;
ALTER FUNCTION solarcommon.timestamp_to_uuid_v7_boundary(timestamp with time zone) PARALLEL SAFE;
ALTER FUNCTION solarcommon.to_basex(bigint, integer) PARALLEL SAFE;
ALTER FUNCTION solarcommon.to_bigint(text) PARALLEL SAFE;
ALTER FUNCTION solarcommon.to_rfc1123_utc(timestamp with time zone) PARALLEL SAFE;
ALTER FUNCTION solarcommon.uuid_to_timestamp_v7(uuid) PARALLEL SAFE;

ALTER FUNCTION solardatm.json_datum_prop_count(jsonb) PARALLEL SAFE;
ALTER FUNCTION solardatm.minute_time_slot(timestamp with time zone, integer) PARALLEL SAFE;
ALTER FUNCTION solardatm.slot_seconds(integer) PARALLEL SAFE;
ALTER FUNCTION solardatm.virutal_stream_id(bigint, text) PARALLEL SAFE;

ALTER FUNCTION solarnet.get_season(date) PARALLEL SAFE;
ALTER FUNCTION solarnet.get_season_monday_start(date) PARALLEL SAFE;

ALTER FUNCTION solarbill.billing_usage_tier_effective_dates() PARALLEL SAFE;
ALTER FUNCTION solarbill.billing_usage_tiers(date) PARALLEL SAFE;

-- ==========================================================================================
-- Aggregates
-- ==========================================================================================

/*
 * ALTER FUNCTION and ALTER AGGREGATE cannot change this on an aggregate, so each must be replaced.
 * None of these define a combine function, so they cannot be used for partial aggregation. They
 * can run in a parallel worker, which is what allows the query around them to use a parallel plan.
 */

ALTER FUNCTION solarcommon.first_sfunc(anyelement, anyelement) PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE solarcommon.first(anyelement) (
    sfunc 		= solarcommon.first_sfunc,
    stype 		= anyelement,
    parallel 	= safe
);

ALTER FUNCTION solardatm.calc_datm_at_sfunc(solardatm.agg_datm_at, solardatm.da_datm, timestamp with time zone) PARALLEL SAFE;
ALTER FUNCTION solardatm.calc_datm_at_ffunc(solardatm.agg_datm_at) PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE solardatm.calc_datm_at(solardatm.da_datm, TIMESTAMP WITH TIME ZONE) (
    stype 		= solardatm.agg_datm_at,
    sfunc 		= solardatm.calc_datm_at_sfunc,
    finalfunc 	= solardatm.calc_datm_at_ffunc,
    initcond	= '(,)',
    parallel 	= safe
);

ALTER FUNCTION solardatm.diff_datm_sfunc(solardatm.agg_datm_diff, solardatm.datm_rec) PARALLEL SAFE;
ALTER FUNCTION solardatm.diff_datm_ffunc(solardatm.agg_datm_diff) PARALLEL SAFE;

CREATE OR REPLACE AGGREGATE solardatm.diff_datm(solardatm.datm_rec) (
    stype 		= solardatm.agg_datm_diff,
    sfunc 		= solardatm.diff_datm_sfunc,
    finalfunc 	= solardatm.diff_datm_ffunc,
    parallel 	= safe
);

-- ==========================================================================================
-- Queries
-- ==========================================================================================

/*
 * This one reads tables, unlike everything above. It is included because it does enough work per
 * stream for a count across many streams to benefit from a parallel plan.
 */

ALTER FUNCTION solardatm.count_datm_time_span_slots(uuid, timestamp with time zone, timestamp with time zone, integer) PARALLEL SAFE;
