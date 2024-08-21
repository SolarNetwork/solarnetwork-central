WITH z AS (
	SELECT m.stream_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta m
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE m.stream_id = ?::uuid
)
UPDATE solardatm.da_datm_meta SET
	node_id = ?
FROM z
WHERE da_datm_meta.stream_id = z.stream_id
RETURNING da_datm_meta.stream_id, node_id, source_id, names_i, names_a, names_s, jdata, 'n' AS kind, z.time_zone