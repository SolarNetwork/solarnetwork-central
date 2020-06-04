-- Must run the following as aa superuser:
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

CREATE SEQUENCE IF NOT EXISTS solaruser.user_node_event_hook_seq;

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_hook (
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

CREATE INDEX IF NOT EXISTS user_node_event_hook_user_topic_idx ON solaruser.user_node_event_hook
	(user_id, topic);

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_task (
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

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_task_result (
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

CREATE INDEX IF NOT EXISTS user_node_event_task_result_hook_idx ON solaruser.user_node_event_task_result
	(hook_id);

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

CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT, edata jsonb)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaruser.user_node_event_task (id, hook_id, node_id, source_id, jdata)
	SELECT uuid_generate_v4() AS id
		, hook_id
		, node
		, source
		, edata AS jdata
	FROM solaruser.find_user_node_event_tasks(node, source, etopic) AS hook_id
$$;

/**
 * FUNCTION solaruser.claim_user_node_event_task
 *
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
