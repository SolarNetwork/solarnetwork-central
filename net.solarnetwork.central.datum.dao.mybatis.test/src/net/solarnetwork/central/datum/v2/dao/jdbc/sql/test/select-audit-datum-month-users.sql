SELECT aud.ts_start AS aud_ts,
	meta.node_id AS aud_node_id,
	meta.source_id AS aud_source_id,
	aud.datum_count AS aud_datum_count,
	aud.datum_hourly_count AS aud_datum_hourly_count,
	aud.datum_daily_count AS aud_datum_daily_count,
	CASE aud.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_monthly_count,
	aud.prop_count AS aud_datum_prop_count,
	aud.datum_q_count AS aud_datum_query_count,
	'Month' AS aud_agg_kind
FROM solardatm.aud_datm_monthly aud
INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = aud.stream_id
INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
WHERE un.user_id = ANY(?)
ORDER BY aud_ts, aud_node_id, aud_source_id
