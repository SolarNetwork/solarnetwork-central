UPDATE solaroscp.oscp_asset_conf SET
	  modified = ?
	, enabled = ?
	, cname = ?
	, cg_id = ?
	, ident = ?
	, audience = ?
	, node_id = ?
	, source_id = ?
	, category = ?
	, phase = ?
	, iprops = ?
	, iprops_stat = ?
	, iprops_unit = ?
	, iprops_mult = ?
	, eprops = ?
	, eprops_stat = ?
	, eprops_unit = ?
	, eprops_mult = ?
	, etype = ?
	, edir = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?