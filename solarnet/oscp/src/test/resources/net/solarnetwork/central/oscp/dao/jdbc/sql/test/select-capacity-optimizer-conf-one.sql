SELECT oco.id,oco.created,oco.modified,oco.user_id,oco.enabled
	,oco.reg_status,oco.cname,oco.url,oco.token,oco.sprops
FROM solaroscp.oscp_co_conf oco
WHERE oco.user_id = ?
AND oco.id = ?
ORDER BY oco.user_id,oco.id