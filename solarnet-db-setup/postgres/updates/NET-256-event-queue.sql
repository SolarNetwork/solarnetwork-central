CREATE SEQUENCE IF NOT EXISTS solaruser.user_node_event_hook_seq;

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_hook (
	id				BIGINT NOT NULL DEFAULT nextval('solaruser.user_node_event_hook_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64) NOT NULL,
	topic 			TEXT NOT NULL,
	cname			CHARACTER VARYING(64) NOT NULL,
	sident			CHARACTER VARYING(128) NOT NULL,
	sprops 			jsonb,
	CONSTRAINT user_node_event_hook_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_hook_user_node_fk FOREIGN KEY (user_id, node_id)
		REFERENCES solaruser.user_node (user_id, node_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS solaruser.user_node_event_task (
	id				uuid NOT NULL,
	hook_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	status			CHARACTER(1) NOT NULL,
	jdata			jsonb,
	success 		BOOLEAN,
	completed 		TIMESTAMP WITH TIME ZONE,
	message			TEXT,
	CONSTRAINT user_node_event_task_pkey PRIMARY KEY (id),
	CONSTRAINT user_node_event_task_hook_fk FOREIGN KEY (hook_id)
		REFERENCES solaruser.user_node_event_hook (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION solaruser.find_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT)
  RETURNS TABLE(
	id				uuid,
	hook_id			BIGINT,
	source_id		CHARACTER VARYING(64)
  ) LANGUAGE SQL VOLATILE AS
$$
	SELECT uuid_generate_v4() AS id
		, h.id AS hook_id
		, source AS source_id
	FROM solaruser.user_node_event_hook h
	WHERE h.topic = etopic
		AND h.node_id = node
		AND (
			(source IS NULL AND h.source_id IS NULL)
			OR (source IS NOT NULL AND source ~ solarcommon.ant_pattern_to_regexp(h.source_id))
		)
$$;

CREATE OR REPLACE FUNCTION solaruser.add_user_node_event_tasks(
	node BIGINT, source CHARACTER VARYING(64), etopic TEXT, edata jsonb)
  RETURNS void LANGUAGE SQL VOLATILE AS
$$
	INSERT INTO solaruser.user_node_event_task (id, hook_id, source_id, status, jdata)
	SELECT id
		, hook_id
		, source_id
		, 'q' AS status
		, edata AS jdata
	FROM solaruser.find_user_node_event_tasks(node, source, etopic)
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
	WHERE status = 'q'
	LIMIT 1
	FOR UPDATE SKIP LOCKED
$$;
