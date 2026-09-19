/**************************************************************************************************
 * FUNCTION solarcommon.jsonb_recursive_merge(jsonb,jsonb,BOOLEAN)
 *
 * Recursively merge JSON objects and arrays (if merge_arrays is TRUE).
 *
 * @param A the "source" JSON
 * @param B the JSON to merge
 * @param merge_arrays if TRUE then also merge arrays, otherwise replace them with values from B
 * @return the merged JSON
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_recursive_merge(A jsonb, B jsonb, merge_arrays BOOLEAN DEFAULT FALSE)
RETURNS jsonb LANGUAGE SQL AS
$$
	SELECT jsonb_object_agg(
		COALESCE(ka, kb),
		CASE
			WHEN va ISNULL THEN vb
			WHEN vb ISNULL THEN va
			WHEN merge_arrays AND jsonb_typeof(va) = 'array' AND jsonb_typeof(vb) = 'array' THEN va || vb
			WHEN jsonb_typeof(va) <> 'object' OR jsonb_typeof(vb) <> 'object' THEN vb
			ELSE solarcommon.jsonb_recursive_merge(va, vb, merge_arrays)
		END
	)
	FROM jsonb_each(A) ta(ka, va)
	FULL JOIN jsonb_each(B) tb(kb, vb) ON ka = kb
$$;
