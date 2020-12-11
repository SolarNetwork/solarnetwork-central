SELECT s.stream_id, s.node_id, s.source_id, s.jdata
	, l.country, l.region, l.state_prov, l.locality, l.postal_code
	, l.address, l.latitude, l.longitude, l.elevation
	, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta s
INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE l.time_zone = ?
	AND l.address IS NULL
	AND l.latitude IS NULL
	AND l.longitude IS NULL
	AND l.elevation IS NULL
	AND l.fts_default @@ solarcommon.plainto_prefix_tsquery(?)
ORDER BY node_id, source_id
