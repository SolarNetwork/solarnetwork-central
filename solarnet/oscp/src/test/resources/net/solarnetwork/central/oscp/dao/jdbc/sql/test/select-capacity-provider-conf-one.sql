SELECT id,created,modified,user_id,enabled,reg_status,cname,url,token,sprops
FROM solaruser.user_oscp_cp_conf uocc
WHERE uocc.user_id = ?
AND uocc.id = ?
ORDER BY uocc.user_id,uocc.id