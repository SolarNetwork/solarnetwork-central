CREATE TABLE solarnet.oauth2_authorized_client (
	user_id						BIGINT NOT NULL,
	client_registration_id 		CHARACTER VARYING(128) NOT NULL,
	principal_name 				CHARACTER VARYING(256) NOT NULL,
	access_token_type 			CHARACTER VARYING(16) NOT NULL,
	access_token_value 			BYTEA NOT NULL,
	access_token_issued_at 		TIMESTAMP WITH TIME ZONE NOT NULL,
	access_token_expires_at 	TIMESTAMP WITH TIME ZONE NOT NULL,
	access_token_scopes 		CHARACTER VARYING(1024) DEFAULT NULL,
	refresh_token_value 		BYTEA DEFAULT NULL,
	refresh_token_issued_at 	TIMESTAMP WITH TIME ZONE DEFAULT NULL,
	created_at 					TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT oauth2_authorized_client_pk PRIMARY KEY (user_id, client_registration_id, principal_name),
	CONSTRAINT oauth2_authorized_client_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);
