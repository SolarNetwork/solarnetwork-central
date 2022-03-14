WITH s AS (
	SELECT s.stream_id, s.loc_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_loc_datm_meta s
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = s.loc_id
	WHERE s.loc_id = ANY(?)
)
SELECT s.stream_id, 
	early.ts AS ts_start,
	late.ts AS ts_end,
	s.loc_id AS obj_id,
	s.source_id,
	s.time_zone,
	'l'::CHARACTER AS kind
FROM s
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.da_datm datum
		WHERE datum.stream_id = s.stream_id
		ORDER BY datum.ts
		LIMIT 1
	) early ON early.stream_id = s.stream_id
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.da_datm datum
		WHERE datum.stream_id = s.stream_id
		ORDER BY datum.ts DESC
		LIMIT 1
	) late ON late.stream_id = s.stream_id
ORDER BY obj_id, source_id
