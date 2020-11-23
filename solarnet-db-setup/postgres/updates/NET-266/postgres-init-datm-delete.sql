/**
 * Delete datum rows matching a set of nodes, sources, and a local date range.
 *
 * The time zones of each node are used to calculate absolute date ranges for each node.
 *
 * @param nodes the list of nodes to resolve absolute dates for
 * @param sources a list of source IDs to include in the results (optional)
 * @param ts_min the starting local date, or the current time if not provided
 * @param ts_max the ending local date, or the current time if not provided
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datm(
	nodes 		BIGINT[],
	sources 	TEXT[],
	ts_min 		TIMESTAMP,
	ts_max 		TIMESTAMP
) RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS $$
DECLARE
	start_date timestamp := COALESCE(ts_min, CURRENT_TIMESTAMP);
	end_date timestamp := COALESCE(ts_max, CURRENT_TIMESTAMP);
	total_count bigint := 0;
	--stale_count bigint := 0;
BEGIN
	WITH nlt AS (
		SELECT COALESCE(l.time_zone, 'UTC') 						AS time_zone,
			start_date AT TIME ZONE COALESCE(l.time_zone, 'UTC') 	AS ts_start,
			end_date AT TIME ZONE COALESCE(l.time_zone, 'UTC') 		AS ts_end,
			array_agg(meta.stream_id) 								AS stream_ids
		FROM solardatm.da_datm_meta meta
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE (
			(COALESCE(cardinality(nodes), 0) = 0 OR meta.node_id = ANY(nodes))
			AND (COALESCE(cardinality(sources), 0) = 0 OR meta.source_id = ANY(sources))
			)
		GROUP BY l.time_zone
	)
	, audit AS (
		UPDATE solardatm.aud_datm_daily d
		SET datum_count = 0, datum_daily_pres = FALSE
		FROM nlt
		WHERE
			-- whole days only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('day', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('day', nlt.ts_start) + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', nlt.ts_end)
			AND d.stream_id = ANY(nlt.stream_ids)
	)
	, hourly AS (
		DELETE FROM solardatm.agg_datm_hourly d
		USING nlt
		WHERE
			-- whole hours (ceil) only; partial to be handled by stale processing
			d.ts_start >= date_trunc('hour', nlt.ts_start) + interval '1 hour'
			AND d.ts_start < date_trunc('hour', nlt.ts_end)
			AND d.stream_id = ANY(nlt.stream_ids)
	)
	, daily AS (
		DELETE FROM solardatm.agg_datm_daily d
		USING nlt
		WHERE
			-- whole days only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('day', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('day', nlt.ts_start) + interval '1 day'
				END
			AND d.ts_start < date_trunc('day', nlt.ts_end)
			AND d.stream_id = ANY(nlt.stream_ids)
	)
	, monthly AS (
		DELETE FROM solardatm.agg_datm_monthly d
		USING nlt
		WHERE
			-- whole months only; partial to be handled by stale processing
			d.ts_start >= CASE
				WHEN date_trunc('month', nlt.ts_start) = nlt.ts_start THEN nlt.ts_start
				ELSE date_trunc('month', nlt.ts_start) + interval '1 month'
				END
			AND d.ts_start < date_trunc('month', nlt.ts_end)
			AND d.stream_id = ANY(nlt.stream_ids)
	)
	DELETE FROM solardatm.da_datm d
	USING nlt
	WHERE
		d.ts >= nlt.ts_start
		AND d.ts < nlt.ts_end
		AND d.stream_id = ANY(nlt.stream_ids);
	GET DIAGNOSTICS total_count = ROW_COUNT;

	-- mark remaining hourly aggregates as stale, so partial hours/days/months recalculated
	WITH meta AS (
		SELECT COALESCE(l.time_zone, 'UTC') AS time_zone,
			meta.stream_id 					AS stream_id
		FROM solardatm.da_datm_meta meta
		LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id
		LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		WHERE (
			(COALESCE(cardinality(nodes), 0) = 0 OR meta.node_id = ANY(nodes))
			AND (COALESCE(cardinality(sources), 0) = 0 OR meta.source_id = ANY(sources))
			)
	)
	, nlt AS (
		SELECT meta.stream_id, start_date AT TIME ZONE meta.time_zone AS ts_start
		FROM meta
		UNION ALL
		SELECT meta.stream_id, end_date AT TIME ZONE meta.time_zone AS ts_start
		FROM meta
	)
	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	SELECT s.stream_id, s.ts_start, 'h' AS agg_kind
	FROM nlt, solardatm.calc_stale_datm(nlt.stream_id, nlt.ts_start) s
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING;

	--GET DIAGNOSTICS stale_count = ROW_COUNT;
	--RAISE NOTICE 'INSERTED % solaragg.agg_stale_datum rows after delete.', stale_count;

	RETURN total_count;
END
$$;


/**
 * Delete rows matching a set of nodes, sources, and a local date range.
 *
 * The `jfilter` parameter must provide the following items:
 *
 * * `nodeIds` - array of node IDs
 * * `sourceIds` - (optional) array of source IDs
 * * `localStartDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 * * `localEndDate` - (optional) string date, yyyy-MM-dd HH:mm format; current time used if not provided
 *
 * @param jfilter.nodes 	the list of nodes to resolve absolute dates for
 * @param jfilter.sources 	a list of source IDs to include in the results (optional)
 * @param jfilter.ts_min 	the starting local date
 * @param jfilter.ts_max 	the ending local date
 */
CREATE OR REPLACE FUNCTION solardatm.delete_datm_for_filter(jfilter jsonb)
	RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	node_ids bigint[] := solarcommon.jsonb_array_to_bigint_array(jfilter->'nodeIds');
	source_ids text[] := solarcommon.json_array_to_text_array(jfilter->'sourceIds');
	ts_min timestamp := jfilter->>'localStartDate';
	ts_max timestamp := jfilter->>'localEndDate';
	total_count bigint := 0;
BEGIN
	SELECT solardatum.delete_datum(node_ids, source_ids, ts_min, ts_max) INTO total_count;
	RETURN total_count;
END
$$;
