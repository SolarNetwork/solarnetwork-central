UPDATE solardatm.da_datm_meta SET
    node_id = ?
WHERE stream_id = ?::uuid
RETURNING stream_id, node_id, source_id