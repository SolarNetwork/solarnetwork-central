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

/**
 * Extract the timestamp from a v7 UUID.
 *
 * See https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#section-5.2
 * No validation is performed to check that the provided UUID is type 7.
 *
 * @param u the v7 UUID to extract the timestamp from
 * @returns the extracted timestamp
 */
CREATE OR REPLACE FUNCTION solarcommon.uuid_to_timestamp_v7(u uuid)
RETURNS TIMESTAMP WITH TIME ZONE LANGUAGE SQL STRICT IMMUTABLE AS
$$
	WITH b AS (
		SELECT uuid_send(u) bu
	)
	SELECT to_timestamp((
	      (get_byte(bu, 0)::BIGINT << 40)
		+ (get_byte(bu, 1)::BIGINT << 32)
		+ (get_byte(bu, 2)::BIGINT << 24)
		+ (get_byte(bu, 3)::BIGINT << 16)
		+ (get_byte(bu, 4)::BIGINT << 8)
		+  get_byte(bu, 5)::BIGINT
		) / 1000.0)
	FROM b
$$;
