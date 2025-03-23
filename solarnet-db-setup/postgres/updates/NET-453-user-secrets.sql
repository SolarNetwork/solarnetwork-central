/**
 * User key pairs.
 *
 * @column user_id 		the ID of the account owner
 * @column skey 		the key pair key
 * @column created		the creation date
 * @column modified		the modification date
 * @column keystore 	the keystore data
 */
CREATE TABLE solaruser.user_keypair (
	user_id			BIGINT NOT NULL,
	skey			CHARACTER VARYING(64) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	keystore		bytea NOT NULL,
	CONSTRAINT user_keypair_pk PRIMARY KEY (user_id, skey),
	CONSTRAINT user_keypair_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);


/**
 * User secrets.
 *
 * @column user_id 		the ID of the account owner
 * @column topic		a grouping ID
 * @column skey 		the secret key
 * @column created		the creation date
 * @column modified		the modification date
 * @column sdata 		the secret value
 */
CREATE TABLE solaruser.user_secret (
	user_id			BIGINT NOT NULL,
	topic 			CHARACTER VARYING(64) NOT NULL,
	skey			CHARACTER VARYING(64) NOT NULL,
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	modified		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	sdata			bytea NOT NULL,
	CONSTRAINT user_secret_pk PRIMARY KEY (user_id, topic, skey),
	CONSTRAINT user_secret_user_fk FOREIGN KEY (user_id)
		REFERENCES solaruser.user_user (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);
