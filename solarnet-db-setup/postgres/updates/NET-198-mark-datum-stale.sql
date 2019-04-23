/**
 * Find hours with datum data in them based on a search criteria.
 *
 * This function can be used to find hour time slots where aggregate
 * data can be computed from.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 * @param start_ts the minimum date (inclusive)
 * @param end_ts the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solaragg.find_datum_hour_slots(
	nodes bigint[],
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone)
  RETURNS TABLE(
	node_id bigint,
	ts_start timestamp with time zone,
	source_id text
  ) LANGUAGE SQL STABLE AS
$$
	SELECT DISTINCT node_id, date_trunc('hour', ts) AS ts_start, source_id
	FROM solardatum.da_datum
	WHERE node_id = ANY(nodes)
		AND source_id = ANY(sources)
		AND ts >= start_ts
		AND ts < end_ts
$$;

/**
 * Find hours with datum data in them based on a search criteria and mark them as "stale" for
 * aggregate processing.
 *
 * This function will insert into the `solaragg.agg_stale_datum` table records for all hours
 * of available data matching the given search criteria.
 *
 * @param nodes the node IDs to find
 * @param sources the source IDs to find
 * @param start_ts the minimum date (inclusive)
 * @param end_ts the maximum date (exclusive)
 */
CREATE OR REPLACE FUNCTION solaragg.mark_datum_stale_hour_slots(
	nodes bigint[],
	sources text[],
	start_ts timestamp with time zone,
	end_ts timestamp with time zone)
  RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaragg.agg_stale_datum
	SELECT dates.ts_start, dates.node_id, dates.source_id, 'h'
	FROM solaragg.find_datum_hour_slots(nodes, sources, start_ts, end_ts) dates
	ON CONFLICT (agg_kind, node_id, ts_start, source_id) DO NOTHING
$$;
