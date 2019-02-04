ALTER TABLE solaruser.user_adhoc_export_task
	DROP CONSTRAINT user_adhoc_export_task_pkey,
	DROP COLUMN export_date,
	ADD CONSTRAINT user_adhoc_export_task_pkey PRIMARY KEY (user_id, task_id);
	
/**
 * Store an ad hoc datum export task for a user.
 *
 * This function will submit an export task via `solarnet.add_datum_export_task()`
 * and return the new primary key for that task. Once submitted it will insert a
 * row into the `solaruser.user_adhoc_export_task` table.
 */
CREATE OR REPLACE FUNCTION solaruser.store_adhoc_export_task(
	usr BIGINT,
	sched CHARACTER(1),
	cfg text
  ) RETURNS uuid LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	t_id uuid;
BEGIN
	t_id := gen_random_uuid();
	PERFORM solarnet.add_datum_export_task(t_id, CURRENT_TIMESTAMP, cfg);
	INSERT INTO solaruser.user_adhoc_export_task
		(user_id, schedule, task_id)
	VALUES
		(usr, sched, t_id);

	RETURN t_id;
END;
$$;


