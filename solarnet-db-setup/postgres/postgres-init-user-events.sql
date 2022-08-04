/******************************************************************************
 * TABLE solaruser.user_event_log
 *
 * Table for events related to a user account.
 */
CREATE TABLE solaruser.user_event_log (
	user_id     BIGINT NOT NULL,
	event_id	UUID NOT NULL,
	tags 		TEXT[] NOT NULL,
	message		TEXT,
	jdata		JSONB,
	CONSTRAINT user_event_log_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add primary key index */
CREATE UNIQUE INDEX user_event_log_pk ON solaruser.user_event_log (user_id, event_id DESC);

/* Add index on tags so we can search efficiently. */
CREATE INDEX user_event_log_tags_idx ON solaruser.user_event_log USING GIN (tags);

/**
 * TABLE solaruser.user_event_log_conf
 *
 * Table for user event log configuration, such as expiration policy.
 */
CREATE TABLE solaruser.user_event_log_conf (
	id			BIGINT NOT NULL DEFAULT nextval('solaruser.user_expire_seq'),
	created		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id     BIGINT NOT NULL,
	expire_days	INTEGER NOT NULL,
	CONSTRAINT user_event_log_conf_pk PRIMARY KEY (id),
	CONSTRAINT user_event_log_conf_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on user_id so we can show all entities for user. */
CREATE INDEX user_event_log_conf_user_idx ON solaruser.user_event_log_conf (user_id);
