INSERT INTO solarev.ocpp_charge_point_status (user_id, cp_id, connected_to)
SELECT user_id, id, ? AS connected_to
FROM solarev.ocpp_charge_point
WHERE user_id = ?
	AND ident = ?
ON CONFLICT (user_id, cp_id) DO UPDATE
SET connected_to = EXCLUDED.connected_to
