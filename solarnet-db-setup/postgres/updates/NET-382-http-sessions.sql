/**
 * HTTP session state information.
 */
CREATE TABLE solaruser.http_session (
	PRIMARY_ID CHAR(36) NOT NULL,
	SESSION_ID CHAR(36) NOT NULL,
	CREATION_TIME BIGINT NOT NULL,
	LAST_ACCESS_TIME BIGINT NOT NULL,
	MAX_INACTIVE_INTERVAL INT NOT NULL,
	EXPIRY_TIME BIGINT NOT NULL,
	PRINCIPAL_NAME VARCHAR(100),
	CONSTRAINT http_session_pk PRIMARY KEY (PRIMARY_ID),
	CONSTRAINT http_session_session_unq UNIQUE (SESSION_ID)
);

CREATE INDEX http_session_exp_idx ON solaruser.http_session (EXPIRY_TIME);
CREATE INDEX http_session_principal_idx ON solaruser.http_session (PRINCIPAL_NAME);

/**
 * HTTP session state attribute information.
 */
CREATE TABLE solaruser.http_session_attributes (
	SESSION_PRIMARY_ID CHAR(36) NOT NULL,
	ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
	ATTRIBUTE_BYTES BYTEA NOT NULL,
	CONSTRAINT http_session_attributes_pk PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
	CONSTRAINT http_session_attributes_http_session_fk FOREIGN KEY (SESSION_PRIMARY_ID)
		REFERENCES solaruser.http_session(PRIMARY_ID) ON DELETE CASCADE
);
