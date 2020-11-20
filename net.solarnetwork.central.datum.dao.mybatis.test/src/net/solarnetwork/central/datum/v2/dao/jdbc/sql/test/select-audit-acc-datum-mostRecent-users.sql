WITH s AS (
	SELECT meta.stream_id, meta.node_id, meta.source_id
	FROM solardatm.da_datm_meta meta
	INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id
	WHERE un.user_id = ANY(?)
)
, r AS (
	SELECT datum.stream_id, datum.ts_start
	FROM s
	INNER JOIN LATERAL (
			SELECT datum.stream_id, datum.ts_start
			FROM solardatm.aud_acc_datm_daily aud
			WHERE datum.stream_id = s.stream_id
			ORDER BY datum.stream_id, datum.ts_start DESC
			LIMIT 1
		) datum ON datum.stream_id = s.stream_id
)
SELECT datum.ts_start AS aud_ts, 
	meta.node_id AS aud_node_id, 
	meta.source_id AS aud_source_id, 
	datum.datum_count AS aud_datum_count, 
	datum.datum_hourly_count AS aud_datum_hourly_count, 
	datum.datum_daily_count AS aud_datum_daily_count, 
	datum.datum_monthly_count AS aud_datum_monthly_count, 
	'RunningTotal' AS aud_agg_kind,
	NULL::bigint AS aud_datum_prop_count, 
	NULL::bigint AS aud_datum_query_count
FROM s
INNER JOIN solardatm.aud_acc_datm_daily datum ON datum.stream_id = r.stream_id 
			AND datum.ts_start = r.ts_start
ORDER BY aud_node_id, aud_source_id, aud_ts DESC
