/**************************************************************************************************
 * FUNCTION solardin.claim_datum_stream_poll_task()
 *
 * "Claim" a poll task from the solardin.cin_datum_stream_poll_task table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the exec_at column.
 *
 * The exec_at column will also be updated to CURRENT_TIMESTAMP, although its original value will
 * be returned.
 *
 * @return the claimed row, if one was able to be claimed
 */
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
		UPDATE solardin.cin_datum_stream_poll_task
		SET status = 'p', exec_at = CURRENT_TIMESTAMP
		WHERE CURRENT OF curs;
		rec.status = 'p';
		RETURN NEXT rec;
	END IF;
	CLOSE curs;
	RETURN;
END
$$;

/**************************************************************************************************
 * FUNCTION solardin.claim_datum_stream_rake_task()
 *
 * "Claim" a rake task from the solardin.cin_datum_stream_rake_task table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the exec_at column, and only one task at a time per ds_id group can be claimed or
 * executing ('p' or 'e' status).
 *
 * The exec_at column will also be updated to CURRENT_TIMESTAMP, although its original value will
 * be returned.
 *
 * @return the claimed row, if one was able to be claimed
 */
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
		UPDATE solardin.cin_datum_stream_rake_task
		SET status = 'p', exec_at = CURRENT_TIMESTAMP
		WHERE CURRENT OF curs;
		rec.status = 'p';
		RETURN NEXT rec;
	END IF;
	CLOSE curs;
	RETURN;
END
$$;
