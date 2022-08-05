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

/******************************************************************************
 * VIEW solaruser.user_event_log_info
 *
 * View of table for events related to a user account with creation date decoded.
 */
CREATE VIEW solaruser.user_event_log_info AS
SELECT user_id, event_id, solarcommon.uuid_to_timestamp_v7(event_id) AS ts, tags, message, jdata
FROM solaruser.user_event_log;
