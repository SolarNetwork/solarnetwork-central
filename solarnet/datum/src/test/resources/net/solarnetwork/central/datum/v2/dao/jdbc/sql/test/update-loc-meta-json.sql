INSERT INTO solardatm.da_loc_datm_meta (loc_id, source_id, jdata)
VALUES (?, ?, ?::jsonb)
ON CONFLICT (loc_id, source_id) DO UPDATE SET
	jdata = EXCLUDED.jdata,
	updated = CURRENT_TIMESTAMP
