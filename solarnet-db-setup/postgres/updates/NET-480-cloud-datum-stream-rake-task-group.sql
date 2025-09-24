CREATE OR REPLACE FUNCTION solardin.claim_datum_stream_rake_task()
	RETURNS SETOF solardin.cin_datum_stream_rake_task LANGUAGE plpgsql VOLATILE ROWS 1 AS
$$
DECLARE
	rec solardin.cin_datum_stream_rake_task;

	-- include ORDER BY here to encourage cin_datum_stream_rake_task_exec_idx to be used
	curs CURSOR FOR SELECT * FROM solardin.cin_datum_stream_rake_task t
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
			FOR UPDATE SKIP LOCKED;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO rec;
	IF FOUND THEN
		UPDATE solardin.cin_datum_stream_rake_task SET status = 'p' WHERE CURRENT OF curs;
		rec.status = 'p';
		RETURN NEXT rec;
	END IF;
	CLOSE curs;
	RETURN;
END
$$;
