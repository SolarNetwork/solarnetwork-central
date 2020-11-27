INSERT INTO solardatm.da_datm_aux (stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta)
VALUES (?, ?, ?::solardatm.da_datm_aux_type, ?, ?::jsonb, ?::jsonb, ?::jsonb)
ON CONFLICT (stream_id, ts, atype) DO UPDATE
SET updated = CURRENT_TIMESTAMP
	, notes = EXCLUDED.notes
	, jdata_af = EXCLUDED.jdata_af
	, jdata_as = EXCLUDED.jdata_as
	, jmeta = EXCLUDED.jmeta
