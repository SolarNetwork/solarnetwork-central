INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	WHERE meta.node_id = ANY(?)
		AND meta.source_id = ANY(?)
)
SELECT datum.stream_id, datum.ts_start, 'd' AS agg_kind
FROM s
INNER JOIN solardatm.find_datm_days(s.stream_id, ?, ?) datum ON TRUE
