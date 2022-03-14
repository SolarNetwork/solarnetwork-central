SELECT datum.stream_id, 
	datum.ts_start AS ts, 
	datum.aud_kind, 
	datum.created
FROM solardatm.aud_stale_datm datum
WHERE datum.aud_kind = ?
ORDER BY datum.aud_kind, ts, datum.stream_id
