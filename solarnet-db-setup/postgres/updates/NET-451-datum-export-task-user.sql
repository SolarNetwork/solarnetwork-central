/**
 * Add user_id and auth_token columns to solarnet.sn_datum_export_task, so the user
 * and authorization token associated with an export task are available directly on
 * the task, instead of having to be resolved by joining back to the solaruser
 * tables that reference the task.
 */

ALTER TABLE solarnet.sn_datum_export_task
	ADD COLUMN user_id BIGINT,
	ADD COLUMN auth_token TEXT;

-- populate the new columns from the user tables that reference the task
UPDATE solarnet.sn_datum_export_task det
SET user_id = s.user_id
	, auth_token = s.auth_token
FROM (
	SELECT det.id
		, COALESCE(uaet.user_id, uedc.user_id) AS user_id
		, COALESCE(uaet.auth_token, uedc.auth_token) AS auth_token
	FROM solarnet.sn_datum_export_task det
	LEFT OUTER JOIN solaruser.user_adhoc_export_task uaet ON uaet.task_id = det.id
	LEFT OUTER JOIN solaruser.user_export_task uet ON uet.task_id = det.id
	LEFT OUTER JOIN solaruser.user_export_datum_conf uedc ON uedc.id = uet.conf_id
) s
WHERE s.id = det.id
	AND s.user_id IS NOT NULL;

-- discard tasks that have no user association at all
DELETE FROM solarnet.sn_datum_export_task WHERE user_id IS NULL;

ALTER TABLE solarnet.sn_datum_export_task
	ALTER COLUMN user_id SET NOT NULL,
	ADD CONSTRAINT datum_export_task_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE;

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
DROP FUNCTION IF EXISTS solarnet.add_datum_export_task(uuid, timestamp with time zone, text);
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


/**
 * Store a datum export task for a user.
 *
 * This function will submit an export task via `solarnet.add_datum_export_task()`
 * and return the new primary key for that task. Once submitted it will insert a
 * row into the `solaruser.user_export_task` table. If a task with the same user,
 * schedule, and export date already exist in `solaruser.user_export_task`, however,
 * the existing task ID (from the `task_id` column) will be returned. Thus this
 * function can be called any number of times for the same task properties without
 * creating duplicate export task records. That fact can be used to allow a job that
 * calls this function to run more frequently than absolutely required for redundancy.
 */
DROP FUNCTION IF EXISTS solaruser.store_export_task(BIGINT, CHARACTER(1), TIMESTAMP WITH TIME ZONE, BIGINT, text);
CREATE OR REPLACE FUNCTION solaruser.store_export_task(
	usr BIGINT,
	sched CHARACTER(1),
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg_id BIGINT,
	cfg text,
	token text DEFAULT NULL
  ) RETURNS uuid LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	t_id uuid;
BEGIN
	SELECT task_id INTO t_id
	FROM solaruser.user_export_task
	WHERE user_id = usr
		AND schedule = sched
		AND export_date = ex_date
	LIMIT 1
	FOR UPDATE;

	IF NOT FOUND THEN
		t_id := gen_random_uuid();
		PERFORM solarnet.add_datum_export_task(t_id, usr, ex_date, cfg, token);
		INSERT INTO solaruser.user_export_task
			(user_id, schedule, export_date, task_id, conf_id)
		VALUES
			(usr, sched, ex_date, t_id, cfg_id);
	END IF;

	RETURN t_id;
END;
$BODY$;


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
	cfg text,
	token text DEFAULT NULL
  ) RETURNS uuid LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	t_id uuid;
BEGIN
	t_id := gen_random_uuid();
	PERFORM solarnet.add_datum_export_task(t_id, usr, CURRENT_TIMESTAMP, cfg, token);
	INSERT INTO solaruser.user_adhoc_export_task
		(user_id, schedule, task_id, auth_token)
	VALUES
		(usr, sched, t_id, token);
	RETURN t_id;
END;
$$;
