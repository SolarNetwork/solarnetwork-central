CREATE SEQUENCE solaruser.user_node_event_hook_seq;

/**
 * A table for user-defined node-related event hooks.
 */
CREATE TABLE solaruser.user_node_event_hook (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_node_event_hook_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_ids		BIGINT[],
	source_ids		CHARACTER VARYING(64)[],
	topic 			TEXT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops 			jsonb,
	CONSTRAINT user_node_event_hook_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_hook_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX user_node_event_hook_user_topic_idx ON solaruser.user_node_event_hook
	(user_id, topic);

/**
 * A table for tasks generated from user-defined node-related event hooks. Each task record
 * represents some job to perform for a given hook.
 */
CREATE TABLE solaruser.user_node_event_task (
	id				uuid NOT NULL,
	hook_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	jdata			jsonb,
	CONSTRAINT user_node_event_task_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_task_hook_fk FOREIGN KEY (hook_id)
		REFERENCES solaruser.user_node_event_hook (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

-- index for foreign key trigger
CREATE INDEX user_node_event_task_hook_idx ON solaruser.user_node_event_task
	(hook_id);

CREATE TABLE solaruser.user_node_event_task_result (
	id				uuid NOT NULL,
	hook_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	status			CHARACTER(1) NOT NULL,
	jdata			jsonb,
	success 		BOOLEAN,
	completed 		TIMESTAMP WITH TIME ZONE,
	message			TEXT,
	CONSTRAINT user_node_event_task_result_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_task_result_hook_fk FOREIGN KEY (hook_id)
		REFERENCES solaruser.user_node_event_hook (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

-- index for foreign key trigger
CREATE INDEX user_node_event_task_result_hook_idx ON solaruser.user_node_event_task_result
	(hook_id);

/**
 * Query for user node event hook tasks for a given node/source/topic.
 *
 * @param node the node ID
 * @param source the source ID
 * @param etopic the node event topic
 * @return a set of `solaruser.user_node_event_hook` primary keys that match the given criteria
 */
CREATE OR REPLACE FUNCTION solaruser.find_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT)
  RETURNS SETOF BIGINT LANGUAGE SQL STABLE AS
$$
	SELECT h.id
	FROM solaruser.user_node_event_hook h, solaruser.user_node un
	WHERE
		un.node_id = node
		AND h.user_id = un.user_id
		AND h.topic = etopic
		AND (
			COALESCE(cardinality(h.node_ids), 0) = 0
			OR h.node_ids @> ARRAY[node]
		)
		AND (
			COALESCE(cardinality(source_ids), 0) = 0
			OR source ~ ANY(
				ARRAY(SELECT array_agg(solarcommon.ant_pattern_to_regexp(s)) AS source_pats FROM unnest(h.source_ids) s)
			)
		)
$$;

/**
 * Create rows in the `solaruser.user_node_event_task` table based on node event hooks found
 * via the `solaruser.find_user_node_event_tasks()` function.
 *
 * This function is intended to be called from an event handler for topic `etopic` within an 
 * application, so that tasks are generated for all relevant hook configurations available.
 *
 * @param node the node ID
 * @param source the source ID
 * @param etopic the node event topic
 * @param edata task data to include with the generated task records
 * @param ts a creation date to associate with the generated task records
 */
CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_tasks(
	node BIGINT
	, source CHARACTER VARYING(64)
	, etopic TEXT
	, edata jsonb
	, ts TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaruser.user_node_event_task (id, hook_id, node_id, source_id, created, jdata)
	SELECT uuid_generate_v4() AS id
		, hook_id
		, node
		, source
		, ts
		, edata AS jdata
	FROM solaruser.find_user_node_event_tasks(node, source, etopic) AS hook_id
$$;

/**
 * "Claim" a user node event, so it may be processed by some external job. This function must be
 * called within a transaction. The returned row will be locked, so that the external job can
 * delete it once complete.
 */
CREATE OR REPLACE FUNCTION solaruser.claim_user_node_event_task()
  RETURNS solaruser.user_node_event_task LANGUAGE SQL VOLATILE AS
$$
	SELECT * FROM solaruser.user_node_event_task
	LIMIT 1
	FOR UPDATE SKIP LOCKED
$$;

/**
 * Move a record from the `solaruser.user_node_event_task` table to the 
 * `solaruser.user_node_event_task_result` table and add result columns.
 *
 * @param task_id 		the task ID to move
 * @param is_success 	`true` if the task completed successfully
 * @param task_status	the status key
 * @param msg			the task message
 * @param completed_at	the date the task completed
 */
CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_task_result(
	task_id uuid
	, is_success BOOLEAN
	, task_status char DEFAULT 'c'
	, msg TEXT DEFAULT NULL
	, completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)
  RETURNS SETOF solaruser.user_node_event_task_result LANGUAGE SQL VOLATILE AS
$$
	WITH task AS (
		DELETE FROM solaruser.user_node_event_task
		WHERE id = task_id
		RETURNING *
	)
	INSERT INTO solaruser.user_node_event_task_result (id, hook_id, node_id, source_id, created, jdata
			, status, success, message, completed)
	SELECT id, hook_id, node_id, source_id, created, jdata
		, task_status, is_success, msg, completed_at
	FROM task
	ON CONFLICT (id) DO UPDATE SET
		status = EXCLUDED.status
		, success = EXCLUDED.success
		, message = EXCLUDED.message
		, completed = EXCLUDED.completed
	RETURNING *
$$;

/**
 * Delete task and task results older than a given date.
 *
 * @param older_than	the date tasks must be older than to delete
 */
CREATE OR REPLACE FUNCTION solaruser.purge_user_node_event_tasks(
	older_than TIMESTAMP WITH TIME ZONE)
  RETURNS BIGINT LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	del_count BIGINT := 0;
	tot_count BIGINT := 0;
BEGIN
	DELETE FROM solaruser.user_node_event_task
	WHERE created < older_than;
	GET DIAGNOSTICS del_count = ROW_COUNT;

	tot_count := del_count;
	
	DELETE FROM solaruser.user_node_event_task_result
	WHERE completed < older_than;
	GET DIAGNOSTICS del_count = ROW_COUNT;
	
	tot_count := tot_count + del_count;
	
	RETURN tot_count;
END;
$$;
