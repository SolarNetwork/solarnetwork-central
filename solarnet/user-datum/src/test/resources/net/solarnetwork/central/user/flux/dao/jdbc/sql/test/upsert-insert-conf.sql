INSERT INTO solaruser.user_flux_agg_pub_settings (
	created,modified,user_id,node_ids,source_ids,publish,retain
)
VALUES (?,?,?,?,?,?,?)
ON CONFLICT (user_id, id) DO UPDATE
	SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
		, node_ids = EXCLUDED.node_ids
		, source_ids = EXCLUDED.source_ids
		, publish = EXCLUDED.publish
		, retain = EXCLUDED.retain
