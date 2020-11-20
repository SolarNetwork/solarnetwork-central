WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
	WHERE un.user_id = ANY(?)
)
SELECT datum.ts_start AS aud_ts, 
	NULL::bigint AS aud_node_id, 
	NULL::text AS aud_source_id, 
	'RunningTotal' AS aud_agg_kind, 
	SUM(datum.datum_count) AS aud_datum_count, 
	NULL::bigint AS aud_datum_prop_count, 
	NULL::bigint AS aud_datum_query_count, 
	SUM(datum.datum_hourly_count) AS aud_datum_hourly_count, 
	SUM(datum.datum_daily_count) AS aud_datum_daily_count, 
	SUM(datum.datum_monthly_count) AS aud_datum_monthly_count 
FROM s
INNER JOIN solardatm.aud_acc_datm_daily datum ON datum.stream_id = s.stream_id
GROUP BY datum.ts_start
ORDER BY aud_ts
