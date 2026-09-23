/**
 * Expand an Ant path pattern into regular expressions that match the pattern's
 * own path prefixes.
 *
 * A path matches one of these when it could still lead to a match for `pat`,
 * the way Spring's AntPathMatcher.matchStart() does. This is used to skip
 * entire object branches while pruning, without having to inspect their leaves.
 *
 * @param pat the Ant path pattern to expand
 * @returns regular expressions for every path prefix of the pattern
 */
CREATE OR REPLACE FUNCTION solarcommon.ant_pattern_prefix_regexps(pat text)
RETURNS text[] LANGUAGE SQL STRICT IMMUTABLE PARALLEL SAFE AS
$$
	SELECT ARRAY(
		SELECT DISTINCT solarcommon.ant_pattern_to_regexp(array_to_string(g.seg[1:i], '/'))
		FROM (SELECT regexp_split_to_array(pat, '/')) g(seg)
		CROSS JOIN LATERAL generate_series(1, cardinality(g.seg)) i
	);
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of regular
 * expressions, preserving the shape of the tree.
 *
 * Object values are descended into when their path matches `node_re`, and all
 * other values are kept when their path matches `leaf_re`; arrays are kept
 * whole. This mirrors SecurityPolicyEnforcer.enforceMetadataPaths(), including
 * its treatment of anything that is not an object as a leaf.
 *
 * Objects left with nothing are dropped, so an object that keeps nothing at all
 * returns NULL rather than an empty object. Callers filtering rows can test the
 * result with IS NOT NULL.
 *
 * Use solarcommon.ant_pattern_to_regexp() for `leaf_re` and
 * solarcommon.ant_pattern_prefix_regexps() for `node_re`, or call
 * solarcommon.jsonb_prune_ant_paths() to do both.
 *
 * @param obj the JSON object to prune
 * @param leaf_re regular expressions matching the paths of values to keep
 * @param node_re regular expressions matching the paths to descend into
 * @param path the path of `obj` itself; pass '' (the default) for the root
 * @returns the pruned object, or NULL if nothing was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_paths(obj jsonb, leaf_re text[],
		node_re text[], path text DEFAULT '')
RETURNS jsonb LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE AS
$$
DECLARE
	result jsonb;
BEGIN
	IF jsonb_typeof(obj) <> 'object' THEN
		RETURN NULL;
	END IF;
	SELECT jsonb_object_agg(k.key, k.val) INTO result FROM (
		SELECT e.key, CASE WHEN jsonb_typeof(e.value) = 'object'
				THEN solarcommon.jsonb_prune_paths(e.value, leaf_re, node_re,
					path || '/' || e.key)
				ELSE e.value END AS val
		FROM jsonb_each(obj) e
		WHERE CASE WHEN jsonb_typeof(e.value) = 'object'
			THEN (path || '/' || e.key) ~ ANY (node_re)
			ELSE (path || '/' || e.key) ~ ANY (leaf_re) END
	) k WHERE k.val IS NOT NULL;
	RETURN result;
END
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of Ant path
 * patterns, preserving the shape of the tree.
 *
 * This is the form used to apply the `userMetadataPaths` and
 * `nodeMetadataPaths` constraints of a security policy to a metadata object.
 *
 * An empty or NULL `pats` means no restriction, and returns `jdata` unchanged,
 * the same way SecurityPolicyEnforcer treats a policy with no metadata paths.
 * Note this means the caller is responsible for not applying an absent policy:
 * join the token with an INNER JOIN so that an unknown token returns no rows.
 *
 * @param jdata the JSON object to prune
 * @param pats the Ant path patterns of the values to keep
 * @returns the pruned object, `jdata` if `pats` is empty, or NULL if nothing
 *          was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_ant_paths(jdata jsonb, pats text[])
RETURNS jsonb LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS
$$
	SELECT CASE WHEN jdata IS NULL OR COALESCE(cardinality(pats), 0) < 1 THEN jdata
		ELSE solarcommon.jsonb_prune_paths(jdata,
			ARRAY(SELECT solarcommon.ant_pattern_to_regexp(p) FROM unnest(pats) p),
			ARRAY(SELECT DISTINCT unnest(solarcommon.ant_pattern_prefix_regexps(p))
				FROM unnest(pats) p),
			'')
		END;
$$;

/**
 * Prune a JSON object down to the values whose paths match a set of Ant path
 * patterns held in a JSON array.
 *
 * This form takes the patterns as they are stored in a security policy, for
 * example `jpolicy -> 'userMetadataPaths'`. A NULL value, or any value that is
 * not a JSON array, means no restriction.
 *
 * @param jdata the JSON object to prune
 * @param pats a JSON array of the Ant path patterns of the values to keep
 * @returns the pruned object, `jdata` if `pats` holds no patterns, or NULL if
 *          nothing was kept
 */
CREATE OR REPLACE FUNCTION solarcommon.jsonb_prune_ant_paths(jdata jsonb, pats jsonb)
RETURNS jsonb LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS
$$
	SELECT CASE WHEN jsonb_typeof(pats) <> 'array' THEN jdata
		ELSE solarcommon.jsonb_prune_ant_paths(jdata,
			ARRAY(SELECT jsonb_array_elements_text(pats)))
		END;
$$;
