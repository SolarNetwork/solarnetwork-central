CREATE OR REPLACE FUNCTION solardatm.mark_stale_datm_hours(
		sid 			UUID,
		start_ts 		TIMESTAMP WITH TIME ZONE,
		end_ts 			TIMESTAMP WITH TIME ZONE
	) RETURNS VOID LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
	(
		SELECT d.stream_id, d.ts_start, 'h'
		FROM solardatm.find_datm_hours(sid, start_ts, end_ts) d
	)
	UNION
	(
		SELECT stream_id, ts_start, 'h'
		FROM solardatm.agg_datm_hourly
		WHERE stream_id = sid
			AND ts_start >= start_ts
			AND ts_start <= end_ts
	)
	ON CONFLICT (agg_kind, stream_id, ts_start) DO NOTHING
$$;
