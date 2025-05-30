SELECT s.stream_id, s.node_id, s.source_id
FROM solardatm.da_datm_meta s
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
INNER JOIN LATERAL (
	SELECT stream_id
	FROM solardatm.da_datm datum
	WHERE datum.stream_id = s.stream_id
	AND datum.ts >= ? AT TIME ZONE COALESCE(l.time_zone, 'UTC')
	AND datum.ts < ? AT TIME ZONE COALESCE(l.time_zone, 'UTC')
	LIMIT 1
) d ON d.stream_id = s.stream_id
WHERE s.node_id = ANY(?)