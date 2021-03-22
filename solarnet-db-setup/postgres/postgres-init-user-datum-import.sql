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
	group_key		TEXT NOT NULL DEFAULT uuid_generate_v4()::TEXT,
	progress		DOUBLE PRECISION NOT NULL DEFAULT 0,
	success 		BOOLEAN,
	load_count		BIGINT NOT NULL DEFAULT 0,
	started 		TIMESTAMP WITH TIME ZONE,
	completed 		TIMESTAMP WITH TIME ZONE,
	message			TEXT,
	config			jsonb NOT NULL,
	CONSTRAINT datum_import_job_pkey PRIMARY KEY (user_id, id),
	CONSTRAINT datum_import_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);


/**************************************************************************************************
 * FUNCTION solarnet.claim_datum_import_job()
 *
 * "Claim" an export task from the solarnet.sn_datum_import_job table that has a status of 'q'
 * and change the status to 'p' and return it. The tasks will be claimed from oldest to newest
 * based on the created column. The `group_key` column will be used as a barrier key such that if
 * any row is claimed or executing no other row with the same group key will be claimed.
 *
 * @return the claimed row, if one was able to be claimed
 */
CREATE OR REPLACE FUNCTION solarnet.claim_datum_import_job()
  RETURNS solarnet.sn_datum_import_job LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	rec solarnet.sn_datum_import_job;
	num INTEGER := 0;

	-- our query here looks for a group with a queued state but without any claimed/executing state
	-- then locks all rows within the group while changing the oldest queued state to claimed
	curs CURSOR FOR
			WITH jg AS (
				SELECT DISTINCT group_key
				FROM solarnet.sn_datum_import_job
				WHERE state IN ('p', 'e')
			), jgg AS (
				SELECT j.*
				FROM solarnet.sn_datum_import_job j
				LEFT OUTER JOIN jg ON jg.group_key = j.group_key
				WHERE j.state = 'q'
					AND jg.group_key IS NULL
				ORDER BY j.created, j.id
				LIMIT 1
			)
			SELECT j.*
			FROM solarnet.sn_datum_import_job j
			INNER JOIN jgg ON jgg.group_key = j.group_key
			WHERE j.state = 'q'
			ORDER BY j.created, j.id
			FOR UPDATE SKIP LOCKED;
BEGIN
	FOR r IN curs LOOP
		IF num = 0 THEN
			UPDATE solarnet.sn_datum_import_job SET state = 'p' WHERE CURRENT OF curs;
			rec := r;
		END IF;
		num := num + 1;
	END LOOP;
	RETURN rec;
END;
$$;


/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_datum_import_jobs(timestamp with time zone)
 *
 * Delete sn_datum_import_job rows that have reached the 'c' state and whose
 * completed date is older than the given date.
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
	WHERE completed < older_date AND state = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;
$$;
