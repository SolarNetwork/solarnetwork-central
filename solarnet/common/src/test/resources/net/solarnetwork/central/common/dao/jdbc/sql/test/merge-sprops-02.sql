UPDATE my_table
SET sprops = solarcommon.jsonb_recursive_merge(COALESCE(sprops, '{}'::jsonb), ?::jsonb, ?)
WHERE user_id = ?
AND id = ?