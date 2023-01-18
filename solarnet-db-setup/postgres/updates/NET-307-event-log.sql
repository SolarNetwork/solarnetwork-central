/**
 * Extract the timestamp from a v7 UUID with microsecond support.
 *
 * See https://www.ietf.org/archive/id/draft-peabody-dispatch-new-uuid-format-04.html#section-5.2
 * No validation is performed to check that the provided UUID is type 7.
 * 
 * Any UUID that encodes a 48-bit millisecond Unix epoch in the highest
 * 6 bytes, and bits 12-2 of bytes 6 & 7 as the microseconds of the UUID
 * can be decoded by this function:
 * 
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           unix_ts_ms                          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |       micros      |rnd|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                        rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                            rand_b                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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
	SELECT to_timestamp(((
		-- convert first 48 bits (millis) to micros
	      (get_byte(bu, 0)::BIGINT << 40)
		+ (get_byte(bu, 1)::BIGINT << 32)
		+ (get_byte(bu, 2)::BIGINT << 24)
		+ (get_byte(bu, 3)::BIGINT << 16)
		+ (get_byte(bu, 4)::BIGINT << 8)
		+  get_byte(bu, 5)::BIGINT
		) * 1000::BIGINT
		-- add bits 52-61 (micros)
		+ ((
		    (
		      (get_byte(bu, 6)::BIGINT << 8)
		    +  get_byte(bu, 7)::BIGINT
		    ) & 4095::BIGINT
		  ) >> 2)
		) / 1000000.0::DECIMAL
	)
	FROM b
$$;

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
