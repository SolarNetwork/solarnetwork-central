SELECT node_id, user_id, private, archived
FROM solaruser.user_node
WHERE node_id = ANY(?)
AND user_id = ANY(?)
ORDER BY node_id