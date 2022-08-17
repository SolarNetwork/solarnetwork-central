SELECT oco.id, oco.created, oco.modified, oco.user_id, oco.enabled
	, oco.fp_id, oco.reg_status, oco.cname, oco.url, ocot.token, oco.sprops
FROM solaroscp.oscp_co_conf oco
LEFT OUTER JOIN solaroscp.oscp_co_token ocot
	ON ocot.user_id = oco.user_id AND ocot.id = oco.id
WHERE oco.user_id = ANY(?)
AND oco.fp_id = ANY(?)
ORDER BY oco.user_id, oco.id