CREATE OR REPLACE FUNCTION solarnet.claim_datum_export_task()
	RETURNS solarnet.sn_datum_export_task LANGUAGE SQL VOLATILE AS
$$
	WITH t AS (
		SELECT id
		FROM solarnet.sn_datum_export_task
		WHERE status = 'q'
		ORDER BY created ASC, id ASC
		LIMIT 1
		FOR UPDATE SKIP LOCKED
	)
	UPDATE solarnet.sn_datum_export_task
	SET status = 'p'
	FROM t
	WHERE sn_datum_export_task.id = t.id
	RETURNING sn_datum_export_task.*
$$;

CREATE OR REPLACE FUNCTION solardin.claim_datum_stream_poll_task()
	RETURNS SETOF solardin.cin_datum_stream_poll_task LANGUAGE SQL VOLATILE ROWS 1 AS
$$
	WITH t AS (
		SELECT t.user_id, t.ds_id, t.exec_at
		FROM solardin.cin_datum_stream_poll_task t
		WHERE t.status = 'q'
		AND t.exec_at <= CURRENT_TIMESTAMP
		ORDER BY t.exec_at
		LIMIT 1
		FOR NO KEY UPDATE SKIP LOCKED
	)
	UPDATE solardin.cin_datum_stream_poll_task
	SET status = 'p', exec_at = CURRENT_TIMESTAMP
	FROM t
	WHERE cin_datum_stream_poll_task.user_id = t.user_id
	AND cin_datum_stream_poll_task.ds_id = t.ds_id
	RETURNING cin_datum_stream_poll_task.user_id
		, cin_datum_stream_poll_task.ds_id
		, cin_datum_stream_poll_task.status
		, t.exec_at
		, cin_datum_stream_poll_task.start_at
		, cin_datum_stream_poll_task.message
		, cin_datum_stream_poll_task.sprops
$$;

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
		SELECT t.user_id, t.id, t.exec_at
		FROM solardin.cin_datum_stream_rake_task t
		INNER JOIN g ON g.user_id = t.user_id AND g.ds_id = t.ds_id
		WHERE t.status = 'q'
		FOR NO KEY UPDATE
	)
	-- update the oldest available task to p
	, t AS (
		SELECT user_id, id, exec_at
		FROM gt
		ORDER BY exec_at, id
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

CREATE OR REPLACE FUNCTION solaruser.claim_datum_delete_job()
  RETURNS solaruser.user_datum_delete_job LANGUAGE SQL VOLATILE AS
$$
	WITH t AS (
		SELECT user_id, id
		FROM solaruser.user_datum_delete_job
		WHERE state = 'q'
		ORDER BY created ASC, id ASC
		LIMIT 1
		FOR NO KEY UPDATE SKIP LOCKED
	)
	UPDATE solaruser.user_datum_delete_job
	SET state = 'p'
	FROM t
	WHERE user_datum_delete_job.user_id = t.user_id
	AND user_datum_delete_job.id = t.id
	RETURNING user_datum_delete_job.*
$$;

CREATE OR REPLACE FUNCTION solarnet.claim_datum_import_job()
	RETURNS solarnet.sn_datum_import_job LANGUAGE SQL VOLATILE AS
$$
	-- identify a group with a q task but without any p,e tasks within it
	WITH g AS (
		SELECT t.user_id, t.group_key
		FROM solarnet.sn_datum_import_job t
		WHERE t.state = 'q'
		AND NOT EXISTS (
			SELECT id
			FROM solarnet.sn_datum_import_job g
			WHERE g.user_id = t.user_id
			AND g.group_key = t.group_key
			AND g.state IN ('p', 'e')
		)
		ORDER BY t.created, t.id
		LIMIT 1
		FOR NO KEY UPDATE SKIP LOCKED
	)
	-- select and lock all q rows within identified group
	, gt AS (
		SELECT t.id, t.created
		FROM solarnet.sn_datum_import_job t
		INNER JOIN g ON g.user_id = t.user_id AND g.group_key = t.group_key
		WHERE t.state = 'q'
		FOR NO KEY UPDATE
	)
	-- update the oldest available task to p
	, t AS (
		SELECT id
		FROM gt
		ORDER BY created, id
		LIMIT 1
	)
	UPDATE solarnet.sn_datum_import_job
	SET state = 'p'
	FROM t
	WHERE sn_datum_import_job.id = t.id
	RETURNING sn_datum_import_job.*
$$;

CREATE OR REPLACE FUNCTION solaruser.claim_node_instr_task()
	RETURNS SETOF solaruser.user_node_instr_task LANGUAGE SQL VOLATILE ROWS 1 AS
$$
	WITH t AS (
		SELECT t.user_id, t.id
		FROM solaruser.user_node_instr_task t
		WHERE t.status = 'q'
		AND t.enabled
		AND t.exec_at <= CURRENT_TIMESTAMP
		ORDER BY t.exec_at
		LIMIT 1
		FOR NO KEY UPDATE SKIP LOCKED
	)
	UPDATE solaruser.user_node_instr_task
	SET status = 'p'
	FROM t
	WHERE user_node_instr_task.user_id = t.user_id
	AND user_node_instr_task.id = t.id
	RETURNING user_node_instr_task.*
$$;
