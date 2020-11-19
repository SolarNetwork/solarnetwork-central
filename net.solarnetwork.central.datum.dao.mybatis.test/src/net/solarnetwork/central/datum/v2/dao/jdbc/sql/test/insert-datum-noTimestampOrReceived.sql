INSERT INTO solardatm.da_datm (stream_id, data_i, data_a, data_s, data_t)
VALUES (?,?,?,?,?)
ON CONFLICT (stream_id, ts) DO UPDATE
SET received = EXCLUDED.received
	, data_i = EXCLUDED.data_i
	, data_a = EXCLUDED.data_a
	, data_s = EXCLUDED.data_s
	, data_t = EXCLUDED.data_t
