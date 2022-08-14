SELECT uoa.id, uoa.created, uoa.modified, uoa.user_id, uoa.enabled, uoa.cname
	, uoa.cg_id, uoa.node_id, uoa.source_id, uoa.category
	, uoa.iprops, uoa.iprops_unit, uoa.iprops_mult, uoa.iprops_phase
	, uoa.eprops, uoa.eprops_unit, uoa.eprops_mult, uoa.etype
	, uoa.sprops
FROM solaruser.user_oscp_asset_conf uoa
WHERE uoa.user_id = ?
AND uoa.cg_id = ?
ORDER BY uoa.user_id, uoa.id