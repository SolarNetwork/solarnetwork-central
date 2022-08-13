SELECT uoco.id,uoco.created,uoco.modified,uoco.user_id,uoco.enabled
	,uoco.reg_status,uoco.cname,uoco.url,uoco.token,uoco.sprops
FROM solaruser.user_oscp_co_conf uoco
WHERE uoco.user_id = ?
AND uoco.id = ?
ORDER BY uoco.user_id,uoco.id