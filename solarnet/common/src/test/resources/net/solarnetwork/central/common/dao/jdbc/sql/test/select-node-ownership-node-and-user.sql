SELECT un.node_id, un.user_id, l.country, l.time_zone, un.private, un.archived
FROM solaruser.user_node un
LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = un.node_id
LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
WHERE un.node_id = ?
AND un.user_id = ?