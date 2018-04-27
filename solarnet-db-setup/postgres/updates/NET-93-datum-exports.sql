/**************************************************************************************************
 * TABLE solarnet.sn_datum_export_task
 *
 * Holds records for datum export tasks, where `status` represents the execution status
 * of the task and `config` holds a complete export configuration document.
 */
CREATE TABLE solarnet.sn_datum_export_task (
	id				uuid NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	export_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	status			CHARACTER(1) NOT NULL,
	config			jsonb NOT NULL,
	success 		BOOLEAN,
	message			TEXT,
	completed 		TIMESTAMP WITH TIME ZONE,
	CONSTRAINT datum_export_task_pkey PRIMARY KEY (id)
);

/**************************************************************************************************
 * FUNCTION solarnet.add_datum_export_task(uuid, timestamp with time zone, text)
 *
 * Insert a new datum export task record.
 *
 * @param uid the UUID of the task
 * @param ex_date the export date of the task
 * @param cfg the complete export configuration document, as JSON
 * @return the status value of the inserted record
 */
CREATE OR REPLACE FUNCTION solarnet.add_datum_export_task(
	uid uuid,
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg text
  ) RETURNS CHARACTER(1) LANGUAGE plpgsql VOLATILE AS
$BODY$
BEGIN
	INSERT INTO solarnet.sn_datum_export_task
		(id, created, export_date, config, status)
	VALUES
		(uid, CURRENT_TIMESTAMP, ex_date, cfg::jsonb, 'q');
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
  RETURNS solarnet.sn_datum_export_task LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	rec solarnet.sn_datum_export_task;
	curs CURSOR FOR SELECT * FROM solarnet.sn_datum_export_task
			WHERE status = 'q'
			ORDER BY created ASC, ID ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO rec;
	IF FOUND THEN
		UPDATE solarnet.sn_datum_export_task SET status = 'p' WHERE CURRENT OF curs;
	END IF;
	CLOSE curs;
	RETURN rec;
END;
$BODY$;

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_datum_export_tasks(timestamp with time zone)
 *
 * Delete sn_datum_export_task rows that have reached the 'c' status, and whose
 * completed date is older than the given date.
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
	RETURN num_rows;
END;
$BODY$;
