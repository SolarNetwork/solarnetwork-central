SELECT uocg.id,uocg.created,uocg.modified,uocg.user_id,uocg.enabled,uocg.cname
	,uocg.ident,uocg.meas_secs,uocg.cp_id,uocg.co_id,uocg.sprops
FROM solaruser.user_oscp_cg_conf uocg
WHERE uocg.user_id = ANY(?)
AND uocg.id = ANY(?)
ORDER BY uocg.user_id,uocg.id