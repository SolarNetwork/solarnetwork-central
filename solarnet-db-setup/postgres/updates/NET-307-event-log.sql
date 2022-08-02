/******************************************************************************
 * TABLE solaruser.user_event_log
 *
 * Table for events related to a user account.
 */
CREATE TABLE solaruser.user_event_log (
	user_id     BIGINT NOT NULL,
	ts			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	id			UUID NOT NULL,
	kind 		CHARACTER VARYING(64) NOT NULL,
	message		TEXT,
	jdata		JSONB,
	CONSTRAINT user_event_log_pk PRIMARY KEY (user_id,ts,id),
	CONSTRAINT user_event_log_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

/* Add index on kind split on '/' so we can search for components. */
CREATE INDEX user_event_log_kind_idx ON solaruser.user_event_log USING GIN (string_to_array(kind,'/'));

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
