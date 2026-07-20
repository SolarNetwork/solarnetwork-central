UPDATE my_table
SET x_props = solarcommon.jsonb_recursive_merge(COALESCE(x_props, '{}'::jsonb), ?::jsonb, ?)
WHERE pk1 = ?
AND pk2 = ?