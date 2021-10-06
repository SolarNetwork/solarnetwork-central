SELECT node_id, user_id, private, archived
FROM solaruser.user_node
WHERE user_id = ?
ORDER BY node_id