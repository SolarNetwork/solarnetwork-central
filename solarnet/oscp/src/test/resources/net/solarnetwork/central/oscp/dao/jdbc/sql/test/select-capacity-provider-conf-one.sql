SELECT uocp.id,uocp.created,uocp.modified,uocp.user_id,uocp.enabled
	,uocp.reg_status,uocp.cname,uocp.url,uocp.token,uocp.sprops
FROM solaruser.user_oscp_cp_conf uocp
WHERE uocp.user_id = ?
AND uocp.id = ?
ORDER BY uocp.user_id,uocp.id