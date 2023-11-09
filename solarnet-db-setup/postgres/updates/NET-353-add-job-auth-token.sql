/*
 * Datum Import
 */

ALTER TABLE solarnet.sn_datum_import_job ADD COLUMN auth_token TEXT;

/*
 * Datum Export
 */

ALTER TABLE solaruser.user_export_datum_conf ADD COLUMN auth_token TEXT;
ALTER TABLE solaruser.user_adhoc_export_task ADD COLUMN auth_token TEXT;

DROP FUNCTION IF EXISTS solaruser.store_adhoc_export_task(BIGINT, CHARACTER(1), TEXT);
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
	PERFORM solarnet.add_datum_export_task(t_id, CURRENT_TIMESTAMP, cfg);
	INSERT INTO solaruser.user_adhoc_export_task
		(user_id, schedule, task_id, auth_token)
	VALUES
		(usr, sched, t_id, token);
	RETURN t_id;
END;
$$;
