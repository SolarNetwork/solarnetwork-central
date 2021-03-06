SELECT s.stream_id, s.node_id, s.source_id, s.jdata
	, l.country, l.region, l.state_prov, l.locality, l.postal_code
	, l.address, l.latitude, l.longitude, l.elevation
	, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_datm_meta s
INNER JOIN solarnet.sn_node n ON n.node_id = s.node_id
INNER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE l.country = ?
	AND l.region = ?
	AND l.state_prov = ?
	AND l.locality = ?
	AND l.postal_code = ?
	AND l.time_zone = ?
	AND l.address = ?
ORDER BY node_id, source_id
