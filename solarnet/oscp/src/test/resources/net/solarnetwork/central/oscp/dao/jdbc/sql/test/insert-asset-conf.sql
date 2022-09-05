INSERT INTO solaroscp.oscp_asset_conf (
	  created, modified, user_id, enabled, cname
	, cg_id, ident, audience, node_id, source_id, category
	, iprops, iprops_unit, iprops_mult, iprops_phase
	, eprops, eprops_unit, eprops_mult, etype
	, sprops
)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb)