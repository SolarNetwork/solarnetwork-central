SELECT stream_id, obj_id, source_id, names_i, names_a, names_s, jdata, kind, time_zone
FROM unnest(?) s(stream_id)
INNER JOIN solardatm.find_metadata_for_stream(s.stream_id) m ON TRUE
