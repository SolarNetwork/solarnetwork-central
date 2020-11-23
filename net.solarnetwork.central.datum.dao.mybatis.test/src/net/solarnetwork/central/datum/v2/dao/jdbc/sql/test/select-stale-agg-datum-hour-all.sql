SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.agg_kind, 
	datum.created
FROM solardatm.agg_stale_datm datum
WHERE datum.agg_kind = ?
ORDER BY datum.aud_kind, ts, datum.stream_id
