/**
 * View of all valid node IDs for a given token, as an array.
 *
 * This will filter out any node IDs not present on the token policy `nodeIds` array.
 * Additionally, archived nodes are filtered out.
 *
 * Typical query is:
 *
 *     SELECT node_id FROM solaruser.user_auth_token_node_ids
 *     WHERE auth_token = 'token-id'
 */
CREATE OR REPLACE VIEW solaruser.user_auth_token_node_ids AS
SELECT t.auth_token,
 	t.user_id,
 	t.token_type,
	t.jpolicy,
	array_agg(un.node_id) AS node_ids
FROM solaruser.user_auth_token t
JOIN solaruser.user_node un ON un.user_id = t.user_id
WHERE un.archived = FALSE 
	AND t.status = 'Active'::solaruser.user_auth_token_status 
	AND ((t.jpolicy -> 'nodeIds') IS NULL OR (t.jpolicy -> 'nodeIds') @> un.node_id::text::jsonb)
GROUP BY t.auth_token, t.user_id;
