SELECT s.stream_id, s.loc_id, s.source_id, s.jdata
	, l.country, l.region, l.state_prov, l.locality, l.postal_code
	, l.address, l.latitude, l.longitude, l.elevation
	, COALESCE(l.time_zone, 'UTC') AS time_zone
FROM solardatm.da_loc_datm_meta s
INNER JOIN solarnet.sn_loc l ON l.id = s.loc_id
WHERE l.country = ?
	AND solarcommon.json_array_to_text_array(s.jdata -> 't') @> ?
ORDER BY loc_id, source_id
