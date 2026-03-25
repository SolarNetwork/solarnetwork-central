WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id, COALESCE(l.time_zone, 'UTC') AS time_zone
	FROM solardatm.da_datm_meta s
	LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
	LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
	WHERE s.node_id = ANY(?)
)
SELECT datum.stream_id, 
	datum.ts, 
	datum.received, 
	datum.data_i, 
	datum.data_a, 
	datum.data_s, 
	datum.data_t
FROM s
INNER JOIN LATERAL (
		SELECT datum.*
		FROM solardatm.da_datm datum
		WHERE datum.stream_id = s.stream_id
		-- this style lateral join was found to execute fastest under Timescale's ChunkAppend scan
		-- but was only selected if the ORDER BY was by time only, even though the index is defined
		-- as (stream_id, ts)
		AND datum.ts < ? AT TIME ZONE s.time_zone
		ORDER BY datum.ts DESC
		LIMIT 1
	) datum ON datum.stream_id = s.stream_id
ORDER BY datum.stream_id, ts
