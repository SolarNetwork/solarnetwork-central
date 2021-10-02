WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	WHERE s.node_id = ANY(?)
)
, r AS (
	SELECT s.stream_id, MAX(latest.ts_start) AS ts_max
	FROM s
		, unnest(ARRAY['h','d','M']) AS agg
		, solardatm.find_agg_time_greatest(s.stream_id, agg.agg) latest
	GROUP BY s.stream_id
)
SELECT datum.stream_id,
	CURRENT_TIMESTAMP AS ts,
	(solardatm.rollup_agg_data(
		(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data
		ORDER BY datum.ts_start)).*
FROM r, solardatm.find_agg_datm_running_total(r.stream_id, ?, r.ts_max) datum
GROUP BY datum.stream_id
ORDER BY datum.stream_id
