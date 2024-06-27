SELECT user_id, id, created, modified, node_ids, source_ids, publish, retain
FROM solaruser.user_flux_agg_pub_settings
WHERE user_id = ?
	AND node_ids @> ?
	AND source_ids @> ?
ORDER BY user_id, id