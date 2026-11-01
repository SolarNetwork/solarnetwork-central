-- collect the real streams that contributed to each virtual stream result; the filter matches the
-- property filters in the wi and wa CTEs, so the output has the same keys as the datum CTE
, dc AS (
	SELECT
		  stream_id
		, ts
		, array_agg(rnode_id ORDER BY rnode_id, rsource_id) AS rnode_ids
		, array_agg(rsource_id::TEXT ORDER BY rnode_id, rsource_id) AS rsource_ids
	FROM d
	WHERE num_nonnulls(VARIADIC d.data_i) > 0
		OR num_nonnulls(VARIADIC d.data_a) > 0
	GROUP BY stream_id, ts
)
