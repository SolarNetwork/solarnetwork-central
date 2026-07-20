UPDATE my_table
SET x_props = COALESCE(x_props, '{}'::jsonb) || ?::jsonb
WHERE pk1 = ?
AND pk2 = ?