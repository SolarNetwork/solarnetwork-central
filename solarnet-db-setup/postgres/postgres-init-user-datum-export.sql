CREATE SEQUENCE solaruser.user_export_seq;

CREATE TABLE solaruser.user_export_data_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	filter			jsonb,
	CONSTRAINT user_export_data_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_data_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_data_conf_user_idx ON solaruser.user_export_data_conf (user_id);

CREATE TABLE solaruser.user_export_dest_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	CONSTRAINT user_export_dest_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_dest_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_dest_conf_user_idx ON solaruser.user_export_dest_conf (user_id);

CREATE TABLE solaruser.user_export_outp_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops			jsonb,
	compression		CHARACTER(1) NOT NULL,
	CONSTRAINT user_export_outp_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_outp_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_outp_conf_user_idx ON solaruser.user_export_outp_conf (user_id);

CREATE TABLE solaruser.user_export_datum_conf (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_export_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	delay_mins		INTEGER NOT NULL,
	schedule		CHARACTER(1) NOT NULL,
	min_export_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	auth_token		TEXT,
	data_conf_id	BIGINT,
	dest_conf_id	BIGINT,
	outp_conf_id	BIGINT,
	CONSTRAINT user_export_datum_conf_pkey PRIMARY KEY (id),
	CONSTRAINT user_export_datum_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	CONSTRAINT user_export_datum_conf_data_fk FOREIGN KEY (data_conf_id)
		REFERENCES solaruser.user_export_data_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_export_datum_conf_dest_fk FOREIGN KEY (dest_conf_id)
		REFERENCES solaruser.user_export_dest_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_export_datum_conf_outp_fk FOREIGN KEY (outp_conf_id)
		REFERENCES solaruser.user_export_outp_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_export_datum_conf_user_idx ON solaruser.user_export_datum_conf (user_id);

/**********************
 * Export tasks
 *********************/

/**************************************************************************************************
 * TABLE solaruser.user_export_task
 *
 * Holds records for user initiated datum export tasks to allow tracking the status
 * of those tasks at the user level.
 */
CREATE TABLE solaruser.user_export_task (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	schedule		CHARACTER(1) NOT NULL,
	export_date		TIMESTAMP WITH TIME ZONE NOT NULL,
	task_id			uuid NOT NULL,
	conf_id			BIGINT NOT NULL,
	CONSTRAINT user_export_task_pkey PRIMARY KEY (user_id, schedule, export_date),
	CONSTRAINT user_export_task_datum_export_task_fk
		FOREIGN KEY (task_id) REFERENCES solarnet.sn_datum_export_task (id)
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT user_export_task_user_export_datum_conf_fk FOREIGN KEY (conf_id)
		REFERENCES solaruser.user_export_datum_conf (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

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
CREATE OR REPLACE FUNCTION solaruser.store_export_task(
	usr BIGINT,
	sched CHARACTER(1),
	ex_date TIMESTAMP WITH TIME ZONE,
	cfg_id BIGINT,
	cfg text
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
		PERFORM solarnet.add_datum_export_task(t_id, ex_date, cfg);
		INSERT INTO solaruser.user_export_task
			(user_id, schedule, export_date, task_id, conf_id)
		VALUES
			(usr, sched, ex_date, t_id, cfg_id);
	END IF;

	RETURN t_id;
END;
$BODY$;

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_user_export_tasks(timestamp with time zone)
 *
 * Delete user_export_task rows whose related sn_datum_export_task have reached the 'c' status and
 * completed date is older than the given date.
 *
 * @param older_date The maximum date to delete tasks for.
 * @return The number of rows deleted.
 */
CREATE OR REPLACE FUNCTION solaruser.purge_completed_user_export_tasks(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$BODY$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solaruser.user_export_task
	USING solarnet.sn_datum_export_task
	WHERE task_id = sn_datum_export_task.id
		AND sn_datum_export_task.completed < older_date
		AND sn_datum_export_task.status = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;
$BODY$;


/**************************************************************************************************
 * TABLE solaruser.user_adhoc_export_task
 *
 * Holds records for user initiated ad hoc datum export tasks to allow tracking the status
 * of those tasks at the user level.
 */
CREATE TABLE solaruser.user_adhoc_export_task (
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	schedule		CHARACTER(1) NOT NULL,
	task_id			uuid NOT NULL,
	auth_token 		TEXT,
	CONSTRAINT user_adhoc_export_task_pkey PRIMARY KEY (user_id, task_id),
	CONSTRAINT user_adhoc_export_task_datum_export_task_fk
		FOREIGN KEY (task_id) REFERENCES solarnet.sn_datum_export_task (id)
		ON UPDATE NO ACTION ON DELETE CASCADE
);

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
	PERFORM solarnet.add_datum_export_task(t_id, CURRENT_TIMESTAMP, cfg);
	INSERT INTO solaruser.user_adhoc_export_task
		(user_id, schedule, task_id, auth_token)
	VALUES
		(usr, sched, t_id, token);
	RETURN t_id;
END;
$$;

/**************************************************************************************************
 * FUNCTION solarnet.purge_completed_user_adhoc_export_tasks(timestamp with time zone)
 *
 * Delete user_adhoc_export_task rows whose related sn_datum_export_task have reached the 'c' status and
 * completed date is older than the given date.
 *
 * @param older_date The maximum date to delete tasks for.
 * @return The number of rows deleted.
 */
CREATE OR REPLACE FUNCTION solaruser.purge_completed_user_adhoc_export_tasks(older_date timestamp with time zone)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	num_rows BIGINT := 0;
BEGIN
	DELETE FROM solaruser.user_adhoc_export_task
	USING solarnet.sn_datum_export_task
	WHERE task_id = sn_datum_export_task.id
		AND sn_datum_export_task.completed < older_date
		AND sn_datum_export_task.status = 'c';
	GET DIAGNOSTICS num_rows = ROW_COUNT;
	RETURN num_rows;
END;
$$;
