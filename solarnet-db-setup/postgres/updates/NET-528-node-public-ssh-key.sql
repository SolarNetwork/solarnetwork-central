/**
 * Look up a node's public SSH key.
 *
 * The key is extracted from the `sn_node_meta` table at the `m/ssh-public-key`
 * path, as text.
 *
 * @param node the ID of the node to get the key for
 * @returns the node's public SSH key, or NULL
 */
CREATE OR REPLACE FUNCTION solarnet.get_node_public_ssh_key(node BIGINT)
RETURNS text LANGUAGE SQL STRICT STABLE AS
$$
	SELECT jsonb_extract_path_text(jdata, 'm',  'ssh-public-key')
	FROM solarnet.sn_node_meta
	WHERE node_id = node
$$;
