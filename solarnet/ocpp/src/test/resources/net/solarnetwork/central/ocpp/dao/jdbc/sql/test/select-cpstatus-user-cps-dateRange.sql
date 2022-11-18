SELECT created, user_id, cp_id, connected_to, connected_date
FROM solarev.ocpp_charge_point_status
WHERE user_id = ?
	AND cp_id = ANY(?)
	AND connected_date >= ?
	AND connected_date < ?
ORDER BY user_id, cp_id