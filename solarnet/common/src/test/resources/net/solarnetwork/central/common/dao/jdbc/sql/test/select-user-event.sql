SELECT uel.user_id,uel.event_id,uel.tags,uel.message,uel.jdata
FROM solaruser.user_event_log uel
WHERE uel.user_id = ?
	AND uel.tags @> ?
	AND uel.event_id >= ?
	AND uel.event_id < ?
ORDER BY uel.user_id,uel.event_id