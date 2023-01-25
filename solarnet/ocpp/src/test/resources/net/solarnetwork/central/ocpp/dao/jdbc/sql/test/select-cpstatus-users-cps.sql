SELECT created, user_id, cp_id, connected_to, session_id, connected_date
FROM solarev.ocpp_charge_point_status
WHERE user_id = ANY(?)
	AND cp_id = ANY(?)
ORDER BY user_id, cp_id