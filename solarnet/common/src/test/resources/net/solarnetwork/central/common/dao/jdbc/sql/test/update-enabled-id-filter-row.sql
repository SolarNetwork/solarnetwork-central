UPDATE example.foo
SET enabled = ?, modified = CURRENT_TIMESTAMP
WHERE pk1 = ?
	AND pk2 = ?