SELECT oac.id, oac.created, oac.modified, oac.user_id, oac.enabled, oac.cname
	, oac.cg_id, oac.audience, oac.node_id, oac.source_id, oac.category
	, oac.iprops, oac.iprops_unit, oac.iprops_mult, oac.iprops_phase
	, oac.eprops, oac.eprops_unit, oac.eprops_mult, oac.etype
	, oac.sprops
FROM solaroscp.oscp_asset_conf oac
WHERE oac.user_id = ANY(?)
AND oac.cg_id = ANY(?)
ORDER BY oac.user_id, oac.id