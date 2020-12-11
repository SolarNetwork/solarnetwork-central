SELECT stream_id, ts, atype, updated, notes, jdata_af, jdata_as, jmeta
FROM solardatm.da_datm_aux
WHERE stream_id = ? AND ts = ? AND atype = ?::solardatm.da_datm_aux_type
