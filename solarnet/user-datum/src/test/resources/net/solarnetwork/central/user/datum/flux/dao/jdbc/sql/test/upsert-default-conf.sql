INSERT INTO solaruser.user_flux_default_agg_pub_settings (
	created,modified,user_id,publish,retain
)
VALUES (?,?,?,?,?)
ON CONFLICT (user_id) DO UPDATE
	SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
		, publish = EXCLUDED.publish
		, retain = EXCLUDED.retain
