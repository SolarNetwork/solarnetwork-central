SELECT datum.stream_id, 
	datum.ts, 
	datum.received, 
	datum.data_i, 
	datum.data_a, 
	datum.data_s, 
	datum.data_t
FROM solardatm.da_datm datum
WHERE datum.stream_id = ? AND datum.ts = ?
