SELECT nm.node_id, nm.created, nm.updated, nm.jdata
FROM solarnet.sn_node_meta nm
WHERE nm.node_id = ANY(?)
ORDER BY nm.node_id
