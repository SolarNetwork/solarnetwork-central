/**************************************************************************************************
 * TABLE solarnet.sn_datum_export_task
 *
 * Holds records for datum export tasks, where `status` represents the execution status
 * of the task and `config` holds a complete export configuration document. The `user_id`
 * and `auth_token` columns hold the user and authorization token the task was created
 * for, copied from the user configuration at task creation time.
 */
CREATE TABLE solarnet.sn_datum_export_task (
	id				uuid NOT NULL,
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	export_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	status			CHARACTER(1) NOT NULL,
	config			jsonb NOT NULL,
	auth_token 		TEXT,
	success 		BOOLEAN,
	message			TEXT,
	completed 		TIMESTAMP WITH TIME ZONE,
	CONSTRAINT datum_export_task_pkey PRIMARY KEY (id),
	CONSTRAINT datum_export_task_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id to support the ON DELETE CASCADE. */
CREATE INDEX sn_datum_export_task_user_idx ON solarnet.sn_datum_export_task (user_id);

/**************************************************************************************************
 * FUNCTION solarnet.add_datum_export_task(uuid, bigint, timestamp with time zone, text, text)
 *
 * Insert a new datum export task record.
 *
 * @param uid the UUID of the task
 * @param usr the ID of the user that owns the task
 * @param ex_date the export date of the task
 * @param cfg the complete export configuration document, as JSON
 * @param token the ID of the authorization token to restrict the export to, if any
 * @return the status value of the inserted record
 */
CREATE OR REPLACE FUNCTION solarnet.add_datum_export_task(
	uid uuid,
	usr BIGINT,
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg text,
	token text DEFAULT NULL
  ) RETURNS CHARACTER(1) LANGUAGE plpgsql VOLATILE AS
$BODY$
BEGIN
	INSERT INTO solarnet.sn_datum_export_task
		(id, user_id, created, export_date, config, status, auth_token)
	VALUES
		(uid, usr, CURRENT_TIMESTAMP, ex_date, cfg::jsonb, 'q', token);
	RETURN 'q';
END;
$BODY$;

/**************************************************************************************************
 * FUNCTION solarnet.claim_datum_export_task()
 *
 * "Claim" an export task from the solarnet.sn_datum_export_task table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the created column.
 *
 * @return the claimed row, if one was able to be claimed
 */
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

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_datum_export_tasks(timestamp with time zone)
 *
 * Delete sn_datum_export_task rows that have reached the 'c' status, and whose
 * completed date is older than the given date. Additionally, tasks that have been
 * left in the 'p' or 'e' status for more than 10 days are reset to the 'c' status
 * with an error message, so they do not linger forever.
 *
 * @param older_date The maximum date to delete tasks for.
 * @return The number of rows deleted.
 */
CREATE OR REPLACE FUNCTION solarnet.purge_completed_datum_export_tasks(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solarnet.sn_datum_export_task
	WHERE completed < older_date AND status = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;

	-- reset very old abandonded tasks to Completed with error
	UPDATE solarnet.sn_datum_export_task
	SET completed = CURRENT_TIMESTAMP
		, success = FALSE
		, status = 'c'
		, message = 'Abandoned'
	WHERE created < (CURRENT_TIMESTAMP - INTERVAL 'P10D')
	AND status IN ('p', 'e');

	RETURN num_rows;
END;
$BODY$;
