WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	WHERE un.user_id = ANY(?)
)
SELECT datum.ts_start AS aud_ts,
	s.node_id AS aud_node_id,
	s.source_id AS aud_source_id,
	'Month' AS aud_agg_kind,
	datum.datum_count AS aud_datum_count,
	datum.prop_count AS aud_datum_prop_count,
	datum.prop_u_count AS aud_datum_prop_update_count,
	datum.datum_q_count AS aud_datum_query_count,
	datum.datum_hourly_count AS aud_datum_hourly_count,
	datum.datum_daily_count AS aud_datum_daily_count,
	CASE datum.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_monthly_count
FROM s
INNER JOIN solardatm.aud_datm_monthly datum ON datum.stream_id = s.stream_id
ORDER BY aud_ts, aud_node_id, aud_source_id
