SELECT datum.stream_id, 
	datum.agg_kind, 
	datum.created
FROM solardatm.agg_stale_flux datum
LIMIT 1
FOR UPDATE SKIP LOCKED
