SELECT aud.ts_start AS aud_ts,
	meta.node_id AS aud_node_id,
	meta.source_id AS aud_source_id,
	aud.datum_count AS aud_datum_count,
	aud.prop_count AS aud_datum_prop_count,
	aud.datum_q_count AS aud_datum_query_count,
	'Hour' AS aud_agg_kind,
	NULL::bigint AS aud_datum_hourly_count,
	NULL::bigint AS aud_datum_daily_count,
	NULL::bigint AS aud_datum_monthly_count
FROM solardatm.aud_datm_hourly aud
INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = aud.stream_id
INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
WHERE un.user_id = ANY(?)
ORDER BY aud_ts, aud_node_id, aud_source_id
