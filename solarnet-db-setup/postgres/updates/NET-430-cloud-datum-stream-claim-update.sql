DROP INDEX IF EXISTS solardin.cin_datum_stream_poll_task_exec_idx;

CREATE INDEX cin_datum_stream_poll_task_exec_idx ON solardin.cin_datum_stream_poll_task
	(exec_at) WHERE (status = 'q');

CREATE OR REPLACE FUNCTION solardin.claim_datum_stream_poll_task()
	RETURNS SETOF solardin.cin_datum_stream_poll_task LANGUAGE plpgsql VOLATILE ROWS 1 AS
$$
DECLARE
	rec solardin.cin_datum_stream_poll_task;

	-- include ORDER BY here to encourage cin_datum_stream_poll_task_exec_idx to be used
	curs CURSOR FOR SELECT * FROM solardin.cin_datum_stream_poll_task
			WHERE status = 'q'
			AND exec_at <= CURRENT_TIMESTAMP
			ORDER BY exec_at
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO rec;
	IF FOUND THEN
		UPDATE solardin.cin_datum_stream_poll_task SET status = 'p' WHERE CURRENT OF curs;
		rec.status = 'p';
		RETURN NEXT rec;
	END IF;
	CLOSE curs;
	RETURN;
END
$$;
