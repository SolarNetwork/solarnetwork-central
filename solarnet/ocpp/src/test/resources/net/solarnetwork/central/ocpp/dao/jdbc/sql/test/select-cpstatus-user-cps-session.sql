SELECT created, user_id, cp_id, connected_to, session_id, connected_date
FROM solarev.ocpp_charge_point_status
WHERE user_id = ?
	AND cp_id = ANY(?)
	AND session_id = ?
ORDER BY user_id, cp_id