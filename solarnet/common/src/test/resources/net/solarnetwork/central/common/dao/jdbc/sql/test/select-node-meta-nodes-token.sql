SELECT nm.node_id, nm.created, nm.updated, solarcommon.jsonb_prune_ant_paths(nm.jdata, t.jpolicy -> 'nodeMetadataPaths') AS jdata
FROM solarnet.sn_node_meta nm
INNER JOIN solaruser.user_node un ON un.node_id = nm.node_id
INNER JOIN solaruser.user_auth_token_login t ON t.user_id = un.user_id
WHERE nm.node_id = ANY(?)
	AND t.username = ?
	AND solarcommon.jsonb_prune_ant_paths(nm.jdata, t.jpolicy -> 'nodeMetadataPaths') IS NOT NULL
ORDER BY nm.node_id
