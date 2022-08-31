SELECT ocg.id,ocg.created,ocg.modified,ocg.user_id,ocg.enabled,ocg.cname
	,ocg.ident,ocg.cp_meas_secs,ocg.co_meas_secs,ocg.cp_id,ocg.co_id,ocg.sprops
	,ocgcpm.meas_at AS cp_meas_at,ocgcom.meas_at AS co_meas_at
FROM solaroscp.oscp_cg_conf ocg
LEFT OUTER JOIN solaroscp.oscp_cg_cp_meas ocgcpm
	ON ocgcpm.user_id = ocg.user_id AND ocgcpm.cg_id = ocg.id
LEFT OUTER JOIN solaroscp.oscp_cg_co_meas ocgcom
	ON ocgcom.user_id = ocg.user_id AND ocgcom.cg_id = ocg.id
WHERE ocg.user_id = ?
AND ocg.id = ?
ORDER BY ocg.user_id,ocg.id