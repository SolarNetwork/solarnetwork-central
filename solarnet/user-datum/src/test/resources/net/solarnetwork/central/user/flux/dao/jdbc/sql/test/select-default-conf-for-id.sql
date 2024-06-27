SELECT user_id, created, modified, publish, retain
FROM solaruser.user_flux_default_agg_pub_settings
WHERE user_id = ?
