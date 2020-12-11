INSERT INTO solardatm.da_datm_meta (node_id, source_id, jdata)
VALUES (?, ?, ?::jsonb)
ON CONFLICT (node_id, source_id) DO UPDATE SET
	jdata = EXCLUDED.jdata,
	updated = CURRENT_TIMESTAMP
