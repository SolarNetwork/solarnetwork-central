SELECT um.user_id, um.created, um.updated, um.jdata
FROM solaruser.user_meta um
WHERE um.user_id = ANY(?)
ORDER BY um.user_id
