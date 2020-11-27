UPDATE solardatm.da_loc_datm_meta
SET jdata = ?::jsonb
WHERE loc_id = ?
	AND source_id = ?
