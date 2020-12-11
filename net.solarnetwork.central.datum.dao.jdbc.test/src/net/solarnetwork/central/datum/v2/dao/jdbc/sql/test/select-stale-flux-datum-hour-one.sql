SELECT datum.stream_id, 
	datum.agg_kind, 
	datum.created
FROM solardatm.agg_stale_flux datum
WHERE datum.agg_kind = ?
LIMIT 1
