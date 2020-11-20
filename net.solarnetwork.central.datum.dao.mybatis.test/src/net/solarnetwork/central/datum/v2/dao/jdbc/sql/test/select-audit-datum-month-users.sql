WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
	WHERE un.user_id = ANY(?)
)
SELECT datum.ts_start AS aud_ts,
	meta.node_id AS aud_node_id,
	meta.source_id AS aud_source_id,
	datum.datum_count AS aud_datum_count,
	datum.datum_hourly_count AS aud_datum_hourly_count,
	datum.datum_daily_count AS aud_datum_daily_count,
	CASE datum.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_monthly_count,
	datum.prop_count AS aud_datum_prop_count,
	datum.datum_q_count AS aud_datum_query_count,
	'Month' AS aud_agg_kind
FROM s
INNER JOIN solardatm.aud_datm_monthly datum ON datum.stream_id = s.stream_id
ORDER BY aud_ts, aud_node_id, aud_source_id
