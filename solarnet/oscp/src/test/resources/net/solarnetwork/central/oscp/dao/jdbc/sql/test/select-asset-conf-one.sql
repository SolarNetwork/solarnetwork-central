SELECT oac.id, oac.created, oac.modified, oac.user_id, oac.enabled, oac.cname
	, oac.cg_id, oac.ident, oac.audience, oac.node_id, oac.source_id, oac.category, oac.phase
	, oac.iprops, oac.iprops_stat, oac.iprops_unit, oac.iprops_mult
	, oac.eprops,  oac.eprops_stat, oac.eprops_unit, oac.eprops_mult, oac.etype, oac.edir
	, oac.sprops
FROM solaroscp.oscp_asset_conf oac
WHERE oac.user_id = ?
AND oac.id = ?
ORDER BY oac.user_id, oac.id