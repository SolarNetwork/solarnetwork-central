CREATE OR REPLACE FUNCTION solardin.claim_datum_stream_rake_task()
	RETURNS SETOF solardin.cin_datum_stream_rake_task LANGUAGE SQL VOLATILE ROWS 1 AS
$$
	-- identify a datum source group with a q task but without any p,e tasks within it
	WITH g AS (
		SELECT t.user_id, t.ds_id
		FROM solardin.cin_datum_stream_rake_task t
		WHERE t.status = 'q'
		AND t.exec_at <= CURRENT_TIMESTAMP
		AND NOT EXISTS (
			SELECT id FROM solardin.cin_datum_stream_rake_task g
			WHERE g.user_id = t.user_id
			AND g.ds_id = t.ds_id
			AND g.status IN ('p', 'e')
		)
		ORDER BY t.exec_at
		LIMIT 1
		FOR NO KEY UPDATE SKIP LOCKED
	)
	-- select and lock all q rows within identified group
	, gt AS (
		SELECT t.user_id, t.id, t.exec_at, t.start_offset
		FROM solardin.cin_datum_stream_rake_task t
		INNER JOIN g ON g.user_id = t.user_id AND g.ds_id = t.ds_id
		WHERE t.status = 'q'
		FOR NO KEY UPDATE
	)
	-- update the oldest available task to p
	, t AS (
		SELECT user_id, id, exec_at
		FROM gt
		ORDER BY exec_at, start_offset DESC
		LIMIT 1
	)
	UPDATE solardin.cin_datum_stream_rake_task
	SET status = 'p', exec_at = CURRENT_TIMESTAMP
	FROM t
	WHERE cin_datum_stream_rake_task.user_id = t.user_id
	AND cin_datum_stream_rake_task.id = t.id
	RETURNING cin_datum_stream_rake_task.user_id
		, cin_datum_stream_rake_task.id
		, cin_datum_stream_rake_task.ds_id
		, cin_datum_stream_rake_task.status
		, t.exec_at
		, cin_datum_stream_rake_task.start_offset
		, cin_datum_stream_rake_task.message
		, cin_datum_stream_rake_task.sprops
$$;
