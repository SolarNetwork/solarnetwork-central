INSERT INTO solarev.ocpp_charge_point_status (user_id, cp_id, connected_to)
SELECT cp.user_id, cp.id, NULL::TEXT AS connected_to
FROM solarev.ocpp_charge_point cp
INNER JOIN solarev.ocpp_charge_point_status cps 
	ON cps.user_id = cp.user_id AND cps.cp_id = cp.id
WHERE cp.user_id = ?
	AND cp.ident = ?
	AND cps.connected_to = ?
ON CONFLICT (user_id, cp_id) DO UPDATE
SET connected_to = EXCLUDED.connected_to