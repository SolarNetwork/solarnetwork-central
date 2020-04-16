/**
 * Calculate the minimum number of absolute rounded date ranges for a given set of nodes.
 *
 * The time zones of each node are used to group them into rows where all nodes have the
 * same absolute dates.
 * 
 * @param nodes   the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param field   the Postgres date_trunc compatible field to truncate the date on, e.g. 'hour', 'day', 'month', etc.
 * @param ts_min  the start date to truncate
 * @param ts_max  the end date to truncate
 */
CREATE OR REPLACE FUNCTION solarnet.node_source_time_rounded(
	nodes bigint[], sources text[], field text , ts_min timestamp, ts_max timestamp)
RETURNS TABLE(
  ts_start timestamp with time zone,
  ts_end timestamp with time zone,
  time_zone text,
  node_ids bigint[],
  source_ids character varying(64)[]
) LANGUAGE sql STABLE AS $$
	SELECT 
		date_trunc(field, ts_min) AT TIME ZONE nlt.time_zone AS ts_start
		, date_trunc(field, ts_max) AT TIME ZONE nlt.time_zone AS ts_end
		, nlt.time_zone AS time_zone
		, array_agg(DISTINCT nlt.node_id) AS nodes
		, array_agg(DISTINCT s.source_id::character varying(64)) FILTER (WHERE s.source_id IS NOT NULL) AS sources
	FROM solarnet.node_local_time nlt
	LEFT JOIN (
		SELECT unnest(sources) AS source_id
	) s ON TRUE
	WHERE nlt.node_id = ANY(nodes)
	GROUP BY time_zone;
$$;

CREATE OR REPLACE FUNCTION solaragg.datum_agg_agg_sfunc(agg_state jsonb, el jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var helper = require('datum/aggAggregate'),
		aggregator;
	if ( !agg_state && el ) {
		aggregator = helper.default('', 0);
		aggregator.addDatumRecord(el);
		agg_state = aggregator.serialize();
	} else if ( agg_state && el ) {
		aggregator = helper.deserialize(agg_state);
		aggregator.addDatumRecord(el); // mutates agg_state
	}
	return agg_state;
$$;

CREATE OR REPLACE FUNCTION solaragg.datum_agg_agg_finalfunc(agg_state jsonb)
RETURNS jsonb LANGUAGE plv8 IMMUTABLE AS $$
	'use strict';
	var helper = require('datum/aggAggregate');
	return (agg_state ? helper.deserialize(agg_state).finish() : {});
$$;

/**
 * Aggregate aggregated datum records into a higher aggregation level.
 *
 * This aggregate accepts aggregate datum objects, such as those from the `agg_datum_monthly` 
 * table, and combines them into an aggregat datum object, such as for a year. The input
 * object must include`jdata` and `jmeta` properties. For example:
 *
 * ```
 * SELECT d.node_id, d.source_id, solaragg.datum_agg_agg(jsonb_build_object(
 *		'jdata', solarnet.jdata_from_components(d.jdata_i, d.jdata_a, d.jdata_s, d.jdata_t),
 *		'jmeta', d.jmeta) ORDER BY d.ts_start) AS jobj
 * FROM solaragg.agg_datum_monthly d
 * WHERE ...
 * GROUP BY d.node_id, d.source_id 
`* ````
 *
 * The resulting object contains the same `jdata` and `jmeta` properties with the results.
 */
DROP AGGREGATE IF EXISTS solaragg.datum_agg_agg(jsonb);
CREATE AGGREGATE solaragg.datum_agg_agg(jsonb) (
    sfunc = solaragg.datum_agg_agg_sfunc,
    stype = jsonb,
    finalfunc = solaragg.datum_agg_agg_finalfunc
);
