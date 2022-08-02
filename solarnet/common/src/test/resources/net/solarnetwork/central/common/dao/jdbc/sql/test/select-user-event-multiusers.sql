SELECT uel.user_id,uel.ts,uel.id,uel.kind,uel.message,uel.jdata
FROM solaruser.user_event_log uel
WHERE uel.user_id = ANY(?)
	AND string_to_array(uel.kind,'/') @> ?
	AND uel.created >= ?
	AND uel.created < ?
ORDER BY uel.user_id,uel.ts,uel.id