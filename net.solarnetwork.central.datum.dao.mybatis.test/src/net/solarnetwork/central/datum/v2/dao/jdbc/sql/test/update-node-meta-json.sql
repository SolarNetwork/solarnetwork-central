UPDATE solardatm.da_datm_meta
SET jdata = ?::jsonb
WHERE node_id = ?
	AND source_id = ?
