WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	WHERE s.node_id = ?
		AND s.source_id = ?
		AND un.user_id = ?
)
SELECT datum.stream_id,
	datum.ts,
	datum.received,
	datum.data_i,
	datum.data_a,
	datum.data_s,
	datum.data_t
FROM s
INNER JOIN solardatm.da_datm datum ON datum.stream_id = s.stream_id
WHERE datum.ts >= ?
	AND datum.ts < ?
ORDER BY ts DESC
LIMIT ?