SELECT oco.id,oco.created,oco.modified,oco.user_id,oco.enabled
	,oco.reg_status,oco.cname,oco.url,oco.token,oco.sprops
FROM solaroscp.oscp_co_conf oco
WHERE oco.user_id = ANY(?)
AND oco.id = ANY(?)
ORDER BY oco.user_id,oco.id