UPDATE solardatm.da_loc_datm_meta SET
    loc_id = ?
WHERE stream_id = ?::uuid
RETURNING stream_id, loc_id, source_id