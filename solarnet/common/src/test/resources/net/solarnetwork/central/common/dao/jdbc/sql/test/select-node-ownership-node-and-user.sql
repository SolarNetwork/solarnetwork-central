SELECT node_id, user_id, private, archived
FROM solaruser.user_node
WHERE node_id = ?
AND user_id = ?