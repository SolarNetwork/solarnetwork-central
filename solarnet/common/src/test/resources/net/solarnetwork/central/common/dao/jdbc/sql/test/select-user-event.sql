SELECT uel.user_id,uel.ts,uel.id,uel.kind,uel.message,uel.jdata
FROM solaruser.user_event_log uel
WHERE uel.user_id = ?
	AND string_to_array(uel.kind,'/') @> ?
	AND uel.ts >= ?
	AND uel.ts < ?
ORDER BY uel.user_id,uel.ts,uel.id