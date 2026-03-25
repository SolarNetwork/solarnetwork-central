WITH s AS (
	SELECT DISTINCT ON (s.stream_id) s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta_aliased s
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	WHERE un.user_id = ANY(?)
	ORDER BY s.stream_id, s.mtype
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
		ORDER BY datum.ts DESC
		LIMIT 1
	) datum ON datum.stream_id = s.stream_id
ORDER BY datum.stream_id, ts
