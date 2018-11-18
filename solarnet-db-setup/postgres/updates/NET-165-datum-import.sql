/**************************************************************************************************
 * TABLE solarnet.sn_datum_import_job
 *
 * Holds records for datum import jobs, where `status` represents the execution status
 * of the task and `config` holds a complete import configuration document.
 */
CREATE TABLE solarnet.sn_datum_import_job (
	id				uuid NOT NULL,
	user_id			BIGINT NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	import_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	state			CHARACTER(1) NOT NULL,
	success 		BOOLEAN,
	load_count		BIGINT NOT NULL DEFAULT 0,
	completed 		TIMESTAMP WITH TIME ZONE,
	message			TEXT,
	config			jsonb NOT NULL,
	CONSTRAINT datum_import_job_pkey PRIMARY KEY (user_id, id)
);

/**************************************************************************************************
 * FUNCTION solarnet.claim_datum_import_job()
 *
 * "Claim" an export task from the solarnet.sn_datum_import_job table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the created column.
 *
 * @return the claimed row, if one was able to be claimed
 */
CREATE OR REPLACE FUNCTION solarnet.claim_datum_import_job()
  RETURNS solarnet.sn_datum_import_job LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	rec solarnet.sn_datum_import_job;
	curs CURSOR FOR SELECT * FROM solarnet.sn_datum_import_job
			WHERE state = 'q'
			ORDER BY created ASC, ID ASC
			LIMIT 1
			FOR UPDATE SKIP LOCKED;
BEGIN
	OPEN curs;
	FETCH NEXT FROM curs INTO rec;
	IF FOUND THEN
		UPDATE solarnet.sn_datum_import_job SET state = 'p' WHERE CURRENT OF curs;
	END IF;
	CLOSE curs;
	RETURN rec;
END;
$$;

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_datum_import_jobs(timestamp with time zone)
 *
 * Delete sn_datum_import_job rows that have reached the 'c' state and whose
 * completed date is older than the given date, or are in the 's' state and whose
 * creation date is older than the given date.
 *
 * @param older_date The maximum date to delete tasks for.
 * @return The number of rows deleted.
 */
CREATE OR REPLACE FUNCTION solarnet.purge_completed_datum_import_jobs(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solarnet.sn_datum_import_job
	WHERE (
		(completed < older_date AND state = 'c')
		OR
		(created < older_date AND state = 's')
	);
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;
$$;
