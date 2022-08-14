INSERT INTO solaruser.user_oscp_asset_conf (
	  created, modified, user_id, enabled, cname
	, cg_id, node_id, source_id, category
	, iprops, iprops_unit, iprops_mult, iprops_phase
	, eprops, eprops_unit, eprops_mult, etype
	, sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb)