/**
 * Account-wide datum input data, to support "previous" input tracking.
 *
 * @column user_id 		the ID of the account owner
 * @column node_id 		the ID of the datum stream node
 * @column source_id	the ID of the datum stream source
 * @column created		the creation date
 * @column input_data	the input data
 */
CREATE TABLE solardin.din_input_data (
	user_id			BIGINT NOT NULL,
	node_id			BIGINT NOT NULL,
	source_id		CHARACTER VARYING(64) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	input_data		bytea,
	CONSTRAINT din_input_data_pk PRIMARY KEY (user_id, node_id, source_id),
	CONSTRAINT din_input_data_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE solardin.din_endpoint ADD COLUMN track_prev BOOLEAN NOT NULL DEFAULT FALSE;
