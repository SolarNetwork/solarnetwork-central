/*
 * Datum Import
 */

ALTER TABLE solarnet.sn_datum_import_job ADD COLUMN auth_token TEXT;

/*
 * Datum Export
 */

ALTER TABLE solarnet.sn_datum_export_task ADD COLUMN auth_token TEXT;
ALTER TABLE solaruser.user_export_datum_conf ADD COLUMN auth_token TEXT;
ALTER TABLE solaruser.user_adhoc_export_task ADD COLUMN auth_token TEXT;

/**************************************************************************************************
 * FUNCTION solarnet.add_datum_export_task(uuid, timestamp with time zone, text)
 *
 * Insert a new datum export task record.
 *
 * @param uid the UUID of the task
 * @param ex_date the export date of the task
 * @param cfg the complete export configuration document, as JSON
 * @param token the optional auth token
 * @return the status value of the inserted record
 */
CREATE OR REPLACE FUNCTION solarnet.add_datum_export_task(
	uid uuid,
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg text,
	token text DEFAULT NULL
  ) RETURNS CHARACTER(1) LANGUAGE plpgsql VOLATILE AS
$$
BEGIN
	INSERT INTO solarnet.sn_datum_export_task
		(id, created, export_date, config, auth_token, status)
	VALUES
		(uid, CURRENT_TIMESTAMP, ex_date, cfg::jsonb, token, 'q');
	RETURN 'q';
END;
$$;

CREATE OR REPLACE FUNCTION solaruser.store_adhoc_export_task(
	usr BIGINT,
	sched CHARACTER(1),
	cfg text,
	token text DEFAULT NULL
  ) RETURNS uuid LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	t_id uuid;
BEGIN
	t_id := gen_random_uuid();
	PERFORM solarnet.add_datum_export_task(t_id, CURRENT_TIMESTAMP, cfg, token);
	INSERT INTO solaruser.user_adhoc_export_task
		(user_id, schedule, task_id, auth_token)
	VALUES
		(usr, sched, t_id, token);

	RETURN t_id;
END;
$$;
