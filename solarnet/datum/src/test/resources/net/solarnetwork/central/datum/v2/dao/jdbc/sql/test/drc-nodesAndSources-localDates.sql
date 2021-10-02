WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
		AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
)
, r AS (
	SELECT COUNT(*) AS datum_count
	FROM s
	INNER JOIN solardatm.da_datm datum ON datum.stream_id = s.stream_id
	WHERE datum.ts >= ? AT TIME ZONE s.time_zone
		AND datum.ts < ? AT TIME ZONE s.time_zone
)
, h AS (
	SELECT COUNT(*) AS datum_hourly_count
	FROM s
	INNER JOIN solardatm.agg_datm_hourly datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < date_trunc('hour', ?) AT TIME ZONE s.time_zone
)
, d AS (
	SELECT COUNT(*) AS datum_daily_count
	FROM s
	INNER JOIN solardatm.agg_datm_daily datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < date_trunc('day', ?) AT TIME ZONE s.time_zone
)
, m AS (
	SELECT COUNT(*) AS datum_monthly_count
	FROM s
	INNER JOIN solardatm.agg_datm_monthly datum ON datum.stream_id = s.stream_id
	WHERE datum.ts_start >= ? AT TIME ZONE s.time_zone
		AND datum.ts_start < date_trunc('month', ?) AT TIME ZONE s.time_zone
)
SELECT NULL::UUID AS stream_id,
	CURRENT_TIMESTAMP AS ts_start,
	r.datum_count,
	h.datum_hourly_count,
	d.datum_daily_count,
	m.datum_monthly_count
FROM r, h, d, m
