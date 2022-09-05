UPDATE solaroscp.oscp_asset_conf SET
	  modified = ?
	, enabled = ?
	, cname = ?
	, cg_id = ?
	, audience = ?
	, node_id = ?
	, source_id = ?
	, category = ?
	, iprops = ?
	, iprops_unit = ?
	, iprops_mult = ?
	, iprops_phase = ?
	, eprops = ?
	, eprops_unit = ?
	, eprops_mult = ?
	, etype = ?
	, sprops = ?::jsonb
WHERE user_id = ? AND id = ?