WITH z AS (
	SELECT m.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_loc_datm_meta m
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id
	WHERE m.stream_id = ?::uuid
)
UPDATE solardatm.da_loc_datm_meta SET
	loc_id = ?
FROM z
WHERE da_loc_datm_meta.stream_id = z.stream_id
RETURNING da_loc_datm_meta.stream_id, loc_id, source_id, names_i, names_a, names_s, jdata, 'l' AS kind, z.time_zone