/**
 * FUNCTION solaragg.find_datum_for_time_span(bigint, text[], timestamp with time zone, interval, interval)
 * 
 * Find rows in the <b>solardatum.da_datum</b> table necessary to calculate aggregate
 * data for a specific duration of time, node, and set of sources. This function will return
 * all available rows within the specified duration, possibly with some rows <em>before</em> or
 * <em>after</em> the duration to enable calculating the actual aggregate over the duration.
 *
 * This query will also include `solardatum.da_datum_aux` records of type `Reset`. These records
 * will include a `Reset` tag value, and either `final` or `start` detailing which reset value
 * it represents.
 *
 * @param node The ID of the node to search for.
 * @param sources An array of one or more source IDs to search for, any of which may match.
 * @param start_ts The start time of the desired time duration.
 * @param span The interval of the time duration, which starts from <b>start_ts</b>.
 * @param slotsecs The number of seconds per minute time slot to assign output rows to. Must be
 *                 between 60 and 1800 and evenly divide into 1800.
 * @param tolerance An interval representing the maximum amount of time before between, and after
 *                  rows with the same source ID are allowed to be considered <em>consecutive</em>
 *                  for the purposes of calculating the overall aggregate of the time duration.
 *
 * @out ts The <b>solardatum.da_datum.ts</b> value.
 * @out source_id The <b>solardatum.da_datum.source_id</b> value.
 * @out jdata The <b>solardatum.da_datum.jdata</b> value.
 * @returns one or more records
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_for_time_span(node bigint, sources text[], start_ts timestamp with time zone, span interval, tolerance interval DEFAULT '01:00:00'::interval)
RETURNS TABLE(ts timestamp with time zone, source_id text, jdata jsonb) LANGUAGE SQL STABLE ROWS 500 AS
$$
	-- find raw data with support for filtering out "extra" leading/lagging rows from results
	WITH d AS (
		SELECT
			d.ts,
			d.source_id,
			CASE
				WHEN lead(d.ts) over win < start_ts OR lag(d.ts) over win > (start_ts + span)
					THEN TRUE
				ELSE FALSE
			END AS outside,
			solardatum.jdata_from_datum(d) as jdata
		FROM solardatum.da_datum d
		WHERE d.node_id = node
			AND d.source_id = ANY(sources)
			AND d.ts >= start_ts - tolerance
			AND d.ts <= start_ts + span + tolerance
		WINDOW win AS (PARTITION BY d.source_id ORDER BY d.ts)
	)
	-- find all reset records per node, source within [start, final] date ranges, producing pairs
	-- of rows for each matching record, of [FINAL, STARTING] data
	, resets AS (
		SELECT aux.ts - unnest(ARRAY['1 millisecond','0'])::interval AS ts
			, aux.source_id
			, CASE
				WHEN lead(aux.ts) over win < start_ts OR lag(aux.ts) over win > (start_ts + span)
					THEN TRUE
				ELSE FALSE
			END AS outside
			, unnest(ARRAY[solardatum.jdata_from_datum_aux_final(aux), solardatum.jdata_from_datum_aux_start(aux)]) AS jdata
		FROM solardatum.da_datum_aux aux
		WHERE aux.atype = 'Reset'::solardatum.da_datum_aux_type
			AND aux.node_id = node
			AND aux.source_id = ANY(sources)
			AND aux.ts >= start_ts - tolerance
			AND aux.ts < start_ts + span + tolerance
		WINDOW win AS (PARTITION BY aux.source_id ORDER BY aux.ts)
	)
	-- combine raw data with reset pairs
	, combined AS (
		SELECT * FROM d WHERE outside = FALSE
		UNION
		SELECT * FROM resets WHERE outside = FALSE
	)
	SELECT ts, source_id, jdata
	FROM combined
	ORDER BY ts, source_id
$$;
