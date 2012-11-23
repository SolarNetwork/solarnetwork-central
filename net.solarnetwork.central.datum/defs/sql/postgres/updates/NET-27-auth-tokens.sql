CREATE TABLE solaruser.user_auth_token (
	user_id			BIGINT NOT NULL,
	auth_token		CHARACTER(20) NOT NULL,
	auth_secret		CHARACTER VARYING(64) NOT NULL,
	status			CHAR(1) NOT NULL,
	CONSTRAINT user_auth_token_pkey PRIMARY KEY (auth_token, user_id),
	CONSTRAINT user_auth_token_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);
