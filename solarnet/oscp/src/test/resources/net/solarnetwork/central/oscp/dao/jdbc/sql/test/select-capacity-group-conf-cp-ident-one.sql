SELECT ocg.id,ocg.created,ocg.modified,ocg.user_id,ocg.enabled,ocg.cname
	,ocg.ident,ocg.meas_secs,ocg.cp_id,ocg.co_id,ocg.sprops
FROM solaroscp.oscp_cg_conf ocg
WHERE ocg.user_id = ?
	AND ocg.cp_id = ?
	AND ocg.ident = ?
ORDER BY ocg.user_id,ocg.id