UPDATE my_table
SET sprops = COALESCE(sprops, '{}'::jsonb) || ?::jsonb
WHERE user_id = ?
AND id = ?