WITH s AS (
	SELECT s.stream_id, s.node_id, s.source_id
	FROM solardatm.da_datm_meta s
	INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
	WHERE s.node_id = ?
		AND s.source_id ~ solarcommon.ant_pattern_to_regexp(?)
		AND un.user_id = ?
)
SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*
	, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id
FROM s
INNER JOIN solardatm.find_datm_diff_rows(s.stream_id, ?, ?) d ON TRUE
GROUP BY s.stream_id
