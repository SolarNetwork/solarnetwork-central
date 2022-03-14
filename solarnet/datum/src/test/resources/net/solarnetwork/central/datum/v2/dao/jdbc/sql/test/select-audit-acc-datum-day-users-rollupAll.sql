WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	WHERE un.user_id = ANY(?)
)
SELECT NULL::timestamptz AS aud_ts,
	NULL::bigint AS aud_node_id,
	NULL::text AS aud_source_id,
	'RunningTotal' AS aud_agg_kind,
	SUM(datum.datum_count) AS aud_datum_count,
	NULL::bigint AS aud_datum_prop_count,
	NULL::bigint AS aud_datum_prop_update_count,
	NULL::bigint AS aud_datum_query_count,
	SUM(datum.datum_hourly_count) AS aud_datum_hourly_count,
	SUM(datum.datum_daily_count) AS aud_datum_daily_count,
	SUM(datum.datum_monthly_count) AS aud_datum_monthly_count
FROM s
INNER JOIN solardatm.aud_acc_datm_daily datum ON datum.stream_id = s.stream_id
