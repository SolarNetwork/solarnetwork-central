ALTER TABLE solarnet.sn_datum_import_job
	ADD COLUMN group_key TEXT NOT NULL DEFAULT uuid_generate_v4()::TEXT;

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
