SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.agg_kind, 
	datum.created
FROM solardatm.agg_stale_datm datum
ORDER BY datum.aud_kind, ts, datum.stream_id
