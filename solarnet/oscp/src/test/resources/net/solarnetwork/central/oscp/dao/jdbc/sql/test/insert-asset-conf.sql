INSERT INTO solaroscp.oscp_asset_conf (
	  created, modified, user_id, enabled, cname
	, cg_id, ident, audience, node_id, source_id, category, phase
	, iprops, iprops_stat, iprops_unit, iprops_mult
	, eprops, eprops_stat, eprops_unit, eprops_mult, etype, edir
	, sprops
)
VALUES (
	  ?, ?, ?, ?, ?
	, ?, ?, ?, ?, ?, ?, ?
	, ?, ?, ?, ?
	, ?, ?, ?, ?, ?, ?
	, ?::jsonb)